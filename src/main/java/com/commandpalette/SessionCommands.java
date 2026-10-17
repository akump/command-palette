package com.commandpalette;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.QuantityFormatter;

/**
 * Keeps track of when this login began and what the account's xp was then, for a command that reports on it and
 * the side panel's Session tab.
 */
@Singleton
class SessionCommands
{
	/**
	 * The xp gained in one skill.
	 */
	static final class Gain
	{
		private final Skill skill;
		private final int xp;

		Gain(Skill skill, int xp)
		{
			this.skill = skill;
			this.xp = xp;
		}

		Skill getSkill()
		{
			return skill;
		}

		int getXp()
		{
			return xp;
		}
	}

	/**
	 * The session so far.
	 */
	static final class Snapshot
	{
		private final Duration time;
		private final List<Gain> gains;

		Snapshot(Duration time, List<Gain> gains)
		{
			this.time = time;
			this.gains = gains;
		}

		Duration getTime()
		{
			return time;
		}

		/**
		 * Most gained first.
		 */
		List<Gain> getGains()
		{
			return gains;
		}

		long getTotalXp()
		{
			return gains.stream().mapToLong(Gain::getXp).sum();
		}

		/**
		 * The xp an hour so far, or 0 for a session too short to say.
		 */
		long getXpPerHour()
		{
			final long seconds = time.getSeconds();
			return seconds < 60 ? 0 : getTotalXp() * 3600 / seconds;
		}
	}

	private static final int SKILLS_LISTED = 3;

	private final Client client;
	private final ClientThread clientThread;

	private Instant start;
	private int[] startXp;

	@Inject
	SessionCommands(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	void collect(List<Command> out)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			out.add(new Command("client:session", "Session summary", "Show in chat", Icons.CLOCK, () -> clientThread.invoke(() ->
			{
				if (start != null)
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", summarize(
						Duration.between(start, Instant.now()), startXp, client.getSkillExperiences()), null);
				}
			})));
		}
	}

	/**
	 * Hands the session so far, or null when not logged in, to the consumer on the Swing thread.
	 */
	void snapshot(Consumer<Snapshot> then)
	{
		clientThread.invoke(() ->
		{
			final Snapshot snapshot = client.getGameState() != GameState.LOGGED_IN || start == null ? null
				: new Snapshot(Duration.between(start, Instant.now()), gains(startXp, client.getSkillExperiences()));
			SwingUtilities.invokeLater(() -> then.accept(snapshot));
		});
	}

	void reset()
	{
		start = null;
		startXp = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// hopping worlds carries on the same session; going back to the login screen ends it
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			reset();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// stats arrive a moment after logging in
		if (start == null && client.getOverallExperience() > 0)
		{
			start = Instant.now();
			startXp = client.getSkillExperiences().clone();
		}
	}

	static String summarize(Duration time, int[] startXp, int[] xp)
	{
		final Snapshot session = new Snapshot(time, gains(startXp, xp));
		final List<Gain> gained = session.getGains();

		final long minutes = time.toMinutes();
		final StringBuilder summary = new StringBuilder("Session: ")
			.append(minutes >= 60 ? minutes / 60 + "h " : "").append(minutes % 60).append("m logged in, ")
			.append(QuantityFormatter.formatNumber(session.getTotalXp())).append(" xp gained");
		if (!gained.isEmpty())
		{
			summary.append(" (");
			for (int n = 0; n < gained.size() && n < SKILLS_LISTED; n++)
			{
				final Gain gain = gained.get(n);
				summary.append(n > 0 ? ", " : "").append(gain.getSkill().getName()).append(' ')
					.append(QuantityFormatter.formatNumber(gain.getXp()));
			}
			summary.append(gained.size() > SKILLS_LISTED ? ", ...)" : ")");
		}
		return summary.toString();
	}

	/**
	 * The skills with xp gained, most first.
	 */
	static List<Gain> gains(int[] startXp, int[] xp)
	{
		final List<Gain> gained = new ArrayList<>();
		for (Skill skill : Skill.values())
		{
			final int i = skill.ordinal();
			if (skill != Skill.OVERALL && i < xp.length && i < startXp.length && xp[i] > startXp[i])
			{
				gained.add(new Gain(skill, xp[i] - startXp[i]));
			}
		}
		gained.sort((a, b) -> Integer.compare(b.getXp(), a.getXp()));
		return gained;
	}
}
