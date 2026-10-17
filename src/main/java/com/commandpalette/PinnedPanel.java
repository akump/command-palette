package com.commandpalette;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * The side panel's Pinned tab: commands to run with a click, the ones pinned and then the ones recently used, which
 * can be pinned from here.
 */
class PinnedPanel extends SideTab
{
	static final int RECENT_LISTED = 8;

	private final Supplier<List<Command>> commands;
	private final Consumer<Command> run;
	private final Supplier<List<String>> pinned;
	private final Consumer<Command> togglePin;
	private final Supplier<List<String>> recent;

	private final JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 4));

	/**
	 * @param commands  every command the palette would list now
	 * @param run       runs one
	 * @param pinned    the keys of the pinned commands
	 * @param togglePin pins a command, or unpins it
	 * @param recent    the keys of the commands last run, latest first
	 */
	PinnedPanel(Supplier<List<Command>> commands, Consumer<Command> run, Supplier<List<String>> pinned,
		Consumer<Command> togglePin, Supplier<List<String>> recent)
	{
		this.commands = commands;
		this.run = run;
		this.pinned = pinned;
		this.togglePin = togglePin;
		this.recent = recent;

		list.setOpaque(false);
		final JScrollPane scroll = scrolling(list);
		add(scroll, BorderLayout.CENTER);
	}

	@Override
	void refresh()
	{
		final List<Command> all = commands.get();
		final List<String> pinnedKeys = pinned.get();
		final List<Command> pins = RecentCommands.pick(all, pinnedKeys, PinnedCommands.MAX);
		final List<String> unpinnedRecent = new ArrayList<>(recent.get());
		unpinnedRecent.removeAll(pinnedKeys);
		final List<Command> recents = RecentCommands.pick(all, unpinnedRecent, RECENT_LISTED);

		list.removeAll();
		list.add(heading("Pinned"));
		if (pins.isEmpty())
		{
			list.add(note("Pin a command below to keep it here, a click away."));
		}
		pins.forEach(command -> list.add(row(command, true)));

		list.add(heading("Recently used"));
		if (recents.isEmpty())
		{
			list.add(note("The commands you run from the palette show up here, to run again or pin."));
		}
		recents.forEach(command -> list.add(row(command, false)));
		list.revalidate();
		list.repaint();
	}

	JPanel getList()
	{
		return list;
	}

	private JPanel row(Command command, boolean isPinned)
	{
		final JPanel row = row();
		row.setToolTipText(command.getDetail().isEmpty() ? command.getTitle() : command.getDetail());
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		final JLabel title = new JLabel(command.getTitle(), command.getIcon(), JLabel.LEADING);
		title.setForeground(ColorScheme.TEXT_COLOR);
		title.setIconTextGap(6);
		final JLabel detail = new JLabel(command.getDetail());
		detail.setFont(small());
		detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		detail.setBorder(BorderFactory.createEmptyBorder(0, command.getIcon() == null ? 0 : Icons.SIZE + 6, 0, 0));
		final JPanel text = new JPanel(new BorderLayout());
		text.setOpaque(false);
		text.add(title, BorderLayout.NORTH);
		if (!command.getDetail().isEmpty())
		{
			text.add(detail, BorderLayout.SOUTH);
		}

		final JButton pin = new JButton(isPinned ? Icons.PIN_FILLED : Icons.PIN);
		pin.setToolTipText(isPinned ? "Unpin" : "Pin");
		pin.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		pin.setContentAreaFilled(false);
		pin.addActionListener(e ->
		{
			togglePin.accept(command);
			refresh();
		});

		row.add(text, BorderLayout.CENTER);
		row.add(pin, BorderLayout.EAST);
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					run.accept(command);
					refresh();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
		return row;
	}
}
