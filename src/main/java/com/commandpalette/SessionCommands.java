package com.commandpalette;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
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
 * Keeps track of when this login began and what the account's xp was then, for a command that reports on it.
 */
@Singleton
class SessionCommands
{
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
		long total = 0;
		final List<Skill> gained = new ArrayList<>();
		for (Skill skill : Skill.values())
		{
			final int i = skill.ordinal();
			if (skill != Skill.OVERALL && i < xp.length && i < startXp.length && xp[i] > startXp[i])
			{
				total += xp[i] - startXp[i];
				gained.add(skill);
			}
		}
		gained.sort((a, b) -> Integer.compare(xp[b.ordinal()] - startXp[b.ordinal()], xp[a.ordinal()] - startXp[a.ordinal()]));

		final long minutes = time.toMinutes();
		final StringBuilder summary = new StringBuilder("Session: ")
			.append(minutes >= 60 ? minutes / 60 + "h " : "").append(minutes % 60).append("m logged in, ")
			.append(QuantityFormatter.formatNumber(total)).append(" xp gained");
		if (!gained.isEmpty())
		{
			summary.append(" (");
			for (int n = 0; n < gained.size() && n < SKILLS_LISTED; n++)
			{
				final Skill skill = gained.get(n);
				summary.append(n > 0 ? ", " : "").append(skill.getName()).append(' ')
					.append(QuantityFormatter.formatNumber(xp[skill.ordinal()] - startXp[skill.ordinal()]));
			}
			summary.append(gained.size() > SKILLS_LISTED ? ", ...)" : ")");
		}
		return summary.toString();
	}
}
