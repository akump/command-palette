package com.commandpalette;

import java.awt.BorderLayout;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.util.QuantityFormatter;

/**
 * The side panel's Session tab: how long this login has lasted and the xp gained in it, kept up to date.
 */
class SessionPanel extends SideTab
{
	private final Consumer<Consumer<SessionCommands.Snapshot>> snapshot;
	private final Function<Skill, Icon> skillIcon;

	private final JPanel content = new JPanel(new DynamicGridLayout(0, 1, 0, 4));

	/**
	 * @param snapshot  hands the session so far, or null when not logged in, to its argument on the Swing thread
	 * @param skillIcon a skill's small icon
	 */
	SessionPanel(Consumer<Consumer<SessionCommands.Snapshot>> snapshot, Function<Skill, Icon> skillIcon)
	{
		this.snapshot = snapshot;
		this.skillIcon = skillIcon;

		content.setOpaque(false);
		final JScrollPane scroll = scrolling(content);
		add(scroll, BorderLayout.CENTER);
	}

	@Override
	boolean isLive()
	{
		return true;
	}

	@Override
	void refresh()
	{
		snapshot.accept(this::show);
	}

	void show(SessionCommands.Snapshot session)
	{
		content.removeAll();
		content.add(heading("This session"));
		if (session == null)
		{
			content.add(note("Log in to start a session. It is counted from logging in, and carries on across"
				+ " world hops."));
		}
		else
		{
			content.add(stat("Logged in", Durations.format(Math.max(1, session.getTime().getSeconds())), Icons.CLOCK));
			content.add(stat("Xp gained", QuantityFormatter.formatNumber(session.getTotalXp()), null));
			content.add(stat("Xp an hour", session.getXpPerHour() > 0
				? QuantityFormatter.formatNumber(session.getXpPerHour()) : "-", null));

			content.add(heading("By skill"));
			if (session.getGains().isEmpty())
			{
				content.add(note("No xp gained yet."));
			}
			for (SessionCommands.Gain gain : session.getGains())
			{
				content.add(stat(gain.getSkill().getName(), QuantityFormatter.formatNumber(gain.getXp()),
					skillIcon.apply(gain.getSkill())));
			}
		}
		content.revalidate();
		content.repaint();
	}

	JPanel getContent()
	{
		return content;
	}

	private static JPanel stat(String name, String value, Icon icon)
	{
		final JPanel row = row();
		final JLabel label = new JLabel(name, icon, JLabel.LEADING);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setIconTextGap(6);
		final JLabel number = new JLabel(value);
		number.setForeground(ColorScheme.TEXT_COLOR);
		row.add(label, BorderLayout.CENTER);
		row.add(number, BorderLayout.EAST);
		return row;
	}
}
