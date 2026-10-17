package com.commandpalette;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * The side panel's Answers tab: what the palette last worked out, to read again or copy.
 */
class AnswersPanel extends SideTab
{
	private final Answers answers;

	private final JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
	private final JButton clear = new JButton("Clear");

	AnswersPanel(Answers answers)
	{
		this.answers = answers;

		clear.setFont(small());
		clear.addActionListener(e -> answers.clear());
		final JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);
		header.add(heading("Answers"), BorderLayout.CENTER);
		header.add(clear, BorderLayout.EAST);

		final JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setOpaque(false);
		top.add(header, BorderLayout.NORTH);
		top.add(note("Sums, taxes, prices and the like that you work out in the palette, latest first, until the"
			+ " client closes."), BorderLayout.SOUTH);

		list.setOpaque(false);
		final JScrollPane scroll = scrolling(list);

		add(top, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
	}

	@Override
	void refresh()
	{
		list.removeAll();
		final List<Answers.Answer> all = answers.list();
		clear.setEnabled(!all.isEmpty());
		if (all.isEmpty())
		{
			list.add(note("Nothing yet: try typing <b>35m / 4.2k</b> or <b>tax 12m</b> in the palette."));
		}
		for (Answers.Answer answer : all)
		{
			final JPanel row = row();
			final JLabel text = new Wrapping("<html>" + escape(answer.getText()) + "</html>", ROW_TEXT_WIDTH);
			text.setIcon(answer.getIcon());
			text.setForeground(ColorScheme.TEXT_COLOR);
			text.setIconTextGap(6);

			final JButton copy = new JButton(Icons.CLIPBOARD);
			copy.setToolTipText("Copy " + answer.getCopy());
			copy.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			copy.setContentAreaFilled(false);
			copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(answer.getCopy()), null));

			row.add(text, BorderLayout.CENTER);
			row.add(copy, BorderLayout.EAST);
			list.add(row);
		}
		list.revalidate();
		list.repaint();
	}

	JPanel getList()
	{
		return list;
	}
}
