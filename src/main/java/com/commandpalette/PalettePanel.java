package com.commandpalette;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The palette itself: a search field over the list of matching commands.
 */
class PalettePanel extends JPanel
{
	interface Listener
	{
		void run(Command command);

		void cancel();

		/**
		 * The list grew or shrank, so the panel's preferred size changed.
		 */
		void resized();
	}

	private static final int WIDTH = 320;
	private static final int ROW_HEIGHT = 24;
	private static final int MAX_ROWS = 12;

	private static final Border ROW_BORDER = BorderFactory.createEmptyBorder(0, 6, 0, 6);
	// a line above the first row of a section
	private static final Border SECTION_BORDER = BorderFactory.createCompoundBorder(
		BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR),
		BorderFactory.createEmptyBorder(0, 6, 1, 6));

	private static final Icon NO_ICON = new Icon()
	{
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
		}

		@Override
		public int getIconWidth()
		{
			return Icons.SIZE;
		}

		@Override
		public int getIconHeight()
		{
			return Icons.SIZE;
		}
	};

	private final Listener listener;
	private final JTextField search = new JTextField();
	private final DefaultListModel<Command> model = new DefaultListModel<>();
	private final JList<Command> list = new JList<>(model);
	private final JScrollPane scroll = new JScrollPane(list);
	private final JLabel empty = new JLabel("No matching commands", SwingConstants.CENTER);

	private List<Command> recent = Collections.emptyList();
	private List<Command> pinned = Collections.emptyList();
	private List<Command> commands = Collections.emptyList();
	// the rows that start a section after the first, the pinned or the rest, when the list is in sections
	private Set<Integer> sectionStarts = Collections.emptySet();
	private boolean showIcons = true;
	private Function<String, List<Command>> forQuery = query -> Collections.emptyList();

	PalettePanel(Listener listener)
	{
		this.listener = listener;

		setLayout(new BorderLayout(0, 4));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)));

		search.putClientProperty("JTextField.placeholderText", "Type a command");
		// Tab moves through the list rather than out of the field
		search.setFocusTraversalKeysEnabled(false);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				refilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				refilter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				refilter();
			}
		});
		search.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				PalettePanel.this.keyPressed(e);
			}

		});

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFocusable(false);
		list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		list.setFixedCellHeight(ROW_HEIGHT);
		list.setFixedCellWidth(WIDTH);
		list.setCellRenderer(new Row());
		final MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				final int row = rowAt(e);
				if (row >= 0)
				{
					list.setSelectedIndex(row);
				}
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				final int row = rowAt(e);
				if (row >= 0 && e.getButton() == MouseEvent.BUTTON1)
				{
					listener.run(model.get(row));
				}
			}
		};
		list.addMouseListener(mouse);
		list.addMouseMotionListener(mouse);

		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		empty.setPreferredSize(new Dimension(WIDTH, ROW_HEIGHT));

		add(search, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
		add(empty, BorderLayout.SOUTH);
	}

	/**
	 * Start over with these commands and an empty search.
	 *
	 * @param recent    the recently used commands, which are listed first and set apart from the rest
	 * @param pinned    the pinned commands, which are listed after them, set apart too
	 * @param rest      every other command
	 * @param forQuery  further commands made from the text typed, listed after the ones matching it
	 * @param showIcons whether each command's icon is drawn before its name
	 */
	void show(List<Command> recent, List<Command> pinned, List<Command> rest,
		Function<String, List<Command>> forQuery, boolean showIcons)
	{
		this.showIcons = showIcons;
		this.recent = recent;
		this.pinned = pinned;
		this.commands = new ArrayList<>(recent);
		this.commands.addAll(pinned);
		this.commands.addAll(rest);
		this.forQuery = forQuery;
		if (search.getText().isEmpty())
		{
			refilter();
		}
		else
		{
			search.setText("");
		}
	}

	/**
	 * Lists again the commands made from what is typed, which may have changed since it was typed, keeping the
	 * selected row.
	 */
	void requery()
	{
		if (!isShowing())
		{
			return;
		}
		final int selected = list.getSelectedIndex();
		refilter();
		if (selected > 0 && selected < model.size())
		{
			select(selected);
		}
	}

	void focusSearch()
	{
		search.requestFocusInWindow();
	}

	JTextField getSearch()
	{
		return search;
	}

	private void refilter()
	{
		model.clear();
		for (Command command : CommandFilter.filter(commands, search.getText()))
		{
			model.addElement(command);
		}
		for (Command command : forQuery.apply(search.getText()))
		{
			model.addElement(command);
		}
		// a search puts the best matches first, wherever they came from
		sectionStarts = search.getText().isEmpty()
			? sectionStarts(List.of(recent.size(), pinned.size(), model.size() - recent.size() - pinned.size()))
			: Collections.emptySet();
		list.setVisibleRowCount(Math.min(model.size(), MAX_ROWS));
		scroll.setVisible(!model.isEmpty());
		empty.setVisible(model.isEmpty());
		select(0);
		listener.resized();
	}

	/**
	 * The first row of each section after the first, a line drawn above it, leaving out the empty ones.
	 *
	 * @param sizes how many rows each section has, in order
	 */
	static Set<Integer> sectionStarts(List<Integer> sizes)
	{
		final Set<Integer> starts = new HashSet<>();
		int row = 0;
		for (int size : sizes)
		{
			if (size > 0 && row > 0)
			{
				starts.add(row);
			}
			row += size;
		}
		return starts;
	}

	private void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_DOWN:
				move(1);
				break;
			case KeyEvent.VK_UP:
				move(-1);
				break;
			case KeyEvent.VK_TAB:
				move(e.isShiftDown() ? -1 : 1);
				break;
			case KeyEvent.VK_PAGE_DOWN:
				select(Math.min(model.size() - 1, list.getSelectedIndex() + MAX_ROWS));
				break;
			case KeyEvent.VK_PAGE_UP:
				select(Math.max(0, list.getSelectedIndex() - MAX_ROWS));
				break;
			case KeyEvent.VK_ENTER:
				if (list.getSelectedValue() != null)
				{
					listener.run(list.getSelectedValue());
				}
				break;
			case KeyEvent.VK_ESCAPE:
				listener.cancel();
				break;
			default:
				return;
		}
		e.consume();
	}

	// wraps around at either end
	private void move(int by)
	{
		if (!model.isEmpty())
		{
			select(Math.floorMod(list.getSelectedIndex() + by, model.size()));
		}
	}

	private void select(int row)
	{
		if (row >= 0 && row < model.size())
		{
			list.setSelectedIndex(row);
			list.ensureIndexIsVisible(row);
		}
	}

	private int rowAt(MouseEvent e)
	{
		final int row = list.locationToIndex(e.getPoint());
		return row >= 0 && list.getCellBounds(row, row).contains(e.getPoint()) ? row : -1;
	}

	private final class Row extends JPanel implements ListCellRenderer<Command>
	{
		// the least of a row's width a detail is cut down to, however long the name
		private static final double DETAIL_SHARE = 0.35;

		private final JLabel title = new JLabel();
		private final JLabel detail = new JLabel();

		private Row()
		{
			setLayout(new BorderLayout(6, 0));

			title.setIconTextGap(6);
			// the clock of a recently used command goes after its detail, at the row's edge
			detail.setHorizontalTextPosition(SwingConstants.LEFT);
			detail.setIconTextGap(6);
			detail.setFont(FontManager.getRunescapeSmallFont());
			detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

			add(title, BorderLayout.CENTER);
			add(detail, BorderLayout.EAST);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Command> list, Command command, int index,
			boolean selected, boolean focused)
		{
			title.setText(command.getTitle());
			// a command without an icon still takes up the space of one, so that the names line up
			title.setIcon(!showIcons ? null : command.getIcon() == null ? NO_ICON : command.getIcon());
			title.setForeground(selected ? ColorScheme.BRAND_ORANGE : ColorScheme.TEXT_COLOR);
			detail.setText(command.getDetail());
			detail.setIcon(!showIcons ? null : recent.contains(command) ? Icons.RECENT
				: pinned.contains(command) ? Icons.PIN_FILLED : null);
			setBorder(sectionStarts.contains(index) ? SECTION_BORDER : ROW_BORDER);
			fitDetail();
			setBackground(selected ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			return this;
		}

		/**
		 * The name comes first: a long detail, such as a world's, is cut short with "..." to leave room for it, down
		 * to a share of the row so that a long name doesn't hide its detail altogether.
		 */
		private void fitDetail()
		{
			detail.setPreferredSize(null);
			final Insets insets = getBorder().getBorderInsets(this);
			// the palette's width: a component's own WIDTH is ImageObserver's flag, which would shadow it here
			final int room = PalettePanel.WIDTH - insets.left - insets.right - ((BorderLayout) getLayout()).getHgap();
			final Dimension wanted = detail.getPreferredSize();
			final int name = title.getPreferredSize().width;
			if (name + wanted.width > room)
			{
				final int width = Math.max(room - name, (int) (room * DETAIL_SHARE));
				detail.setPreferredSize(new Dimension(Math.min(wanted.width, width), wanted.height));
			}
		}
	}
}
