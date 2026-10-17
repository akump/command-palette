package com.commandpalette;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * The side panel's Timers tab: start one, and see the ones running count down.
 */
class TimersPanel extends SideTab
{
	// the lengths of the buttons that start a timer in one click, in minutes
	private static final int[] QUICK_MINUTES = {1, 5, 10, 30, 60};

	private final Supplier<List<Timers.Status>> running;
	private final Consumer<Timers.Request> start;

	private final JTextField field = new JTextField();
	private final JLabel error = note("");
	private final JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 4));

	/**
	 * @param running the timers running now, soonest done first
	 * @param start   starts a timer
	 */
	TimersPanel(Supplier<List<Timers.Status>> running, Consumer<Timers.Request> start)
	{
		this.running = running;
		this.start = start;

		field.setToolTipText("A length and what it is for, such as 5m herb run");
		field.addActionListener(e -> startTyped());
		final JButton startButton = new JButton("Start");
		startButton.addActionListener(e -> startTyped());
		final JPanel typed = new JPanel(new BorderLayout(6, 0));
		typed.setOpaque(false);
		typed.add(field, BorderLayout.CENTER);
		typed.add(startButton, BorderLayout.EAST);

		final JPanel quick = new JPanel(new GridLayout(1, QUICK_MINUTES.length, 4, 0));
		quick.setOpaque(false);
		for (int minutes : QUICK_MINUTES)
		{
			final JButton button = new JButton(minutes >= 60 ? minutes / 60 + "h" : minutes + "m");
			button.setFont(small());
			button.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
			button.setToolTipText("Start a " + Durations.format(minutes * 60) + " timer,"
				+ " for what is typed above if anything");
			button.addActionListener(e -> startQuick(minutes));
			quick.add(button);
		}

		error.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		error.setVisible(false);

		final JPanel header = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
		header.setOpaque(false);
		header.add(heading("Start a timer"));
		header.add(note("Type a length and what it is for, such as <b>5m herb run</b>, or click a length."));
		// a hidden error takes no room in a border layout, where it would in the grid
		final JPanel entry = new JPanel(new BorderLayout(0, 4));
		entry.setOpaque(false);
		entry.add(typed, BorderLayout.NORTH);
		entry.add(error, BorderLayout.SOUTH);
		header.add(entry);
		header.add(quick);
		header.add(heading("Running"));

		list.setOpaque(false);
		final JScrollPane scroll = scrolling(list);

		add(header, BorderLayout.NORTH);
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
		list.removeAll();
		final List<Timers.Status> timers = running.get();
		if (timers.isEmpty())
		{
			list.add(note("No timers are running."));
		}
		for (Timers.Status timer : timers)
		{
			final JPanel row = row();
			final JLabel name = new JLabel(timer.getRequest().describe());
			name.setForeground(ColorScheme.TEXT_COLOR);
			final JLabel left = new JLabel(Durations.format(timer.getSecondsLeft()) + " left");
			left.setFont(small());
			left.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			final JPanel text = new JPanel(new BorderLayout());
			text.setOpaque(false);
			text.add(name, BorderLayout.NORTH);
			text.add(left, BorderLayout.SOUTH);

			final JButton cancel = new JButton(Icons.CLOSE);
			cancel.setToolTipText("Cancel this timer");
			cancel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			cancel.setContentAreaFilled(false);
			cancel.addActionListener(e ->
			{
				timer.cancel();
				refresh();
			});

			row.add(text, BorderLayout.CENTER);
			row.add(cancel, BorderLayout.EAST);
			list.add(row);
		}
		list.revalidate();
		list.repaint();
	}

	JTextField getField()
	{
		return field;
	}

	JLabel getError()
	{
		return error;
	}

	private void startTyped()
	{
		final Timers.Request request = Timers.parse(field.getText());
		if (request == null)
		{
			error.setText("<html>Start with a length of time, up to a day: <b>5m</b>, <b>1h 30m</b>, <b>90s</b>.</html>");
			error.setVisible(true);
			return;
		}
		started(request);
	}

	private void startQuick(int minutes)
	{
		// whatever is typed is what the timer is for
		final String typed = field.getText().trim();
		final Timers.Request request = Timers.parse(minutes + "m " + typed);
		if (request != null)
		{
			started(request);
		}
	}

	private void started(Timers.Request request)
	{
		start.accept(request);
		field.setText("");
		error.setVisible(false);
		refresh();
	}
}
