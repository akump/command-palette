package com.commandpalette;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

/**
 * The side panel's Worlds tab, which the world commands open: the world run from the palette is scrolled to and
 * highlighted, and the user hops by clicking, since a plugin may only hop from a click in a side panel. Nothing
 * here hops from a key.
 */
class WorldPanel extends SideTab
{
	/**
	 * Which worlds to list.
	 */
	enum Membership
	{
		ALL("All worlds"),
		MEMBERS("Members"),
		FREE("Free");

		private final String label;

		Membership(String label)
		{
			this.label = label;
		}

		boolean test(World world)
		{
			return this == ALL || (this == MEMBERS) == world.getTypes().contains(WorldType.MEMBERS);
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/**
	 * What order to list them in, after the favourites.
	 */
	enum Sort
	{
		NUMBER("By number"),
		PLAYERS("By players");

		private final String label;

		Sort(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	// a row's padding either side, the highlighted one's bar included
	private static final int ROW_SIDES = 15;
	// how wide a row's detail can be: under the name, beside the icon, inside the padding
	private static final int DETAIL_WIDTH = TEXT_WIDTH - ROW_SIDES - (Icons.SIZE + 6);
	private static final Border ROW_BORDER = BorderFactory.createEmptyBorder(4, 9, 4, 6);
	// an orange bar down the left of the highlighted world
	private static final Border HIGHLIGHT_BORDER = BorderFactory.createCompoundBorder(
		BorderFactory.createMatteBorder(0, 3, 0, 0, ColorScheme.BRAND_ORANGE),
		BorderFactory.createEmptyBorder(4, 6, 4, 6));

	private final Runnable refresh;
	private final Consumer<World> hop;
	private final BiConsumer<Integer, Boolean> setFavourite;

	private final IconTextField search = new IconTextField();
	private final JComboBox<Membership> membership = new JComboBox<>(Membership.values());
	private final JComboBox<Sort> sort = new JComboBox<>(Sort.values());
	private final JButton hopButton = new JButton();
	private final JButton backButton = new JButton();
	// replaced whole on each change, as filling the list's own model a row at a time lays the list out for each
	private DefaultListModel<World> model = new DefaultListModel<>();
	private final JList<World> list = new JList<>(model)
	{
		// A list is as wide as its widest row would like, which a long detail made wider than the panel, pushing
		// the player counts out of sight. Kept to the panel's width, the rows wrap instead.
		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}
	};
	private final JScrollPane scroll = new JScrollPane(list);

	// every world, of which the list shows the ones matching the search and filter
	private List<World> worlds = List.of();
	private Function<World, WorldLocation> location = world -> null;
	private IntPredicate favourite = id -> false;
	private int current = -1;
	private int previous = -1;
	private boolean canHop;
	// the highlighted world's number, kept while the list is refreshed, or -1
	private int highlighted = -1;
	private int hovered = -1;
	// each world's detail, worked out once per show rather than for every row drawn: plain to search, and as HTML
	private Map<Integer, String> details = Map.of();
	private Map<Integer, String> htmlDetails = Map.of();
	// the HTML the rows were last laid out with, to tell whether a refresh changed anything
	private Map<Integer, String> shownHtml = Map.of();
	// while the search and filter are reset together, which would each rebuild the list
	private boolean resetting;

	/**
	 * @param refresh      calls {@link #show} with the worlds as they are now
	 * @param hop          hops to the world, on a click
	 * @param setFavourite adds the world of that number to the World Hopper plugin's favourites, or takes it out
	 */
	WorldPanel(Runnable refresh, Consumer<World> hop, BiConsumer<Integer, Boolean> setFavourite)
	{
		this.refresh = refresh;
		this.hop = hop;
		this.setFavourite = setFavourite;

		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 30));
		search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		search.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				filter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				filter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				filter();
			}
		});

		membership.setFont(small());
		membership.addActionListener(e -> filter());
		sort.setFont(small());
		sort.addActionListener(e -> filter());
		final JPanel choices = new JPanel(new GridLayout(1, 2, 6, 0));
		choices.setOpaque(false);
		choices.add(membership);
		choices.add(sort);

		// not focusable, so that Space doesn't press them: only a click hops
		hopButton.setFocusable(false);
		hopButton.addActionListener(e -> hopToHighlighted());
		backButton.setFocusable(false);
		backButton.addActionListener(e -> hopBack());

		final JPanel header = new JPanel(new DynamicGridLayout(0, 1, 0, 6));
		header.setOpaque(false);
		header.add(search);
		header.add(choices);
		header.add(hopButton);
		header.add(backButton);
		header.add(note("Double-click a world, or pick one and click the button."
			+ " Right-click one to add or remove it as a World Hopper favourite."));

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// arrow keys could move the highlight, but Enter mustn't hop, so keep keys away from the list entirely
		list.setFocusable(false);
		list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		list.setCellRenderer(new Row());
		list.addListSelectionListener(e ->
		{
			final World world = list.getSelectedValue();
			if (world != null)
			{
				highlighted = world.getId();
			}
			updateButtons();
		});
		final MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 && rowAt(e) >= 0)
				{
					hopToHighlighted();
				}
			}

			// a menu opens on press on some platforms and on release on others
			@Override
			public void mousePressed(MouseEvent e)
			{
				showMenu(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				showMenu(e);
			}

			@Override
			public void mouseMoved(MouseEvent e)
			{
				setHovered(rowAt(e));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setHovered(-1);
			}
		};
		list.addMouseListener(mouse);
		list.addMouseMotionListener(mouse);

		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		add(header, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
		updateButtons();
	}

	@Override
	void refresh()
	{
		refresh.run();
	}

	/**
	 * @param worlds    the worlds to list, favourites first
	 * @param location  where a world is, or null if unknown
	 * @param favourite whether a world is one of the World Hopper plugin's favourites
	 * @param current   the world logged in to, or -1
	 * @param previous  the world logged in to before it, or -1
	 * @param canHop    whether the game is somewhere it can hop from
	 */
	void show(List<World> worlds, Function<World, WorldLocation> location, IntPredicate favourite, int current,
		int previous, boolean canHop)
	{
		// asked once per world here: it is a settings lookup, and sorting would ask it many times over
		final Set<Integer> favourites = new HashSet<>();
		final Map<Integer, String> details = new HashMap<>();
		final Map<Integer, String> htmlDetails = new HashMap<>();
		for (World world : worlds)
		{
			final boolean isFavourite = favourite.test(world.getId());
			if (isFavourite)
			{
				favourites.add(world.getId());
			}
			final WorldLocation place = location.apply(world);
			final String detail = WorldCommands.detail(world, place, isFavourite, world.getId() == current);
			details.put(world.getId(), detail);
			String html = escape(detail);
			if (place != null)
			{
				// "US West" kept on one line
				html = html.replace(place.getName(), place.getName().replace(" ", "&nbsp;"));
			}
			htmlDetails.put(world.getId(), "<html>" + html + "</html>");
		}
		this.location = location;
		this.favourite = favourites::contains;
		this.current = current;
		this.previous = previous;
		this.canHop = canHop;
		this.worlds = worlds;
		this.details = details;
		this.htmlDetails = htmlDetails;
		filter();
	}

	/**
	 * Picks the world out, scrolled to the middle of the list, ready to be clicked.
	 */
	void highlight(int world)
	{
		// the world mustn't be hidden by an earlier search or filter
		resetting = true;
		try
		{
			search.setText("");
			membership.setSelectedItem(Membership.ALL);
		}
		finally
		{
			resetting = false;
		}
		refresh.run();
		select(world);
		// once the sidebar has opened and laid the list out
		SwingUtilities.invokeLater(() -> scrollTo(list.getSelectedIndex()));
	}

	IconTextField getSearch()
	{
		return search;
	}

	JComboBox<Membership> getMembership()
	{
		return membership;
	}

	JComboBox<Sort> getSort()
	{
		return sort;
	}

	JButton getHopButton()
	{
		return hopButton;
	}

	JButton getBackButton()
	{
		return backButton;
	}

	JList<World> getList()
	{
		return list;
	}

	private void filter()
	{
		if (resetting)
		{
			return;
		}
		final int keep = highlighted;
		final String query = search.getText();
		final Membership membershipShown = (Membership) membership.getSelectedItem();
		final List<World> shown = new ArrayList<>();
		for (World world : ordered(worlds, favourite, (Sort) sort.getSelectedItem()))
		{
			if (membershipShown.test(world) && matches(world, detail(world), query))
			{
				shown.add(world);
			}
		}
		// Opening the tab refreshes it a few times over: the same rows needn't be laid out again, which renders
		// every one of them
		if (!shown.equals(Collections.list(model.elements())) || !htmlDetails.equals(shownHtml))
		{
			final DefaultListModel<World> next = new DefaultListModel<>();
			next.addAll(shown);
			model = next;
			list.setModel(next);
			shownHtml = htmlDetails;
		}
		// a world hidden by the search stays highlighted, to show again when the search is cleared
		select(keep);
		updateButtons();
	}

	/**
	 * The worlds in the order chosen, the favourites still first.
	 */
	static List<World> ordered(List<World> worlds, IntPredicate favourite, Sort sort)
	{
		final List<World> ordered = new ArrayList<>(worlds);
		final Comparator<World> favouritesFirst = Comparator.comparing(world -> !favourite.test(world.getId()));
		ordered.sort(sort == Sort.PLAYERS
			? favouritesFirst.thenComparing(Comparator.comparingInt(World::getPlayers).reversed())
				.thenComparingInt(World::getId)
			: favouritesFirst.thenComparingInt(World::getId));
		return ordered;
	}

	/**
	 * Whether every word of the search is in the world's name or detail: "302", "wint", "members favourite".
	 */
	static boolean matches(World world, String detail, String query)
	{
		final String text = ("World " + world.getId() + " " + detail).toLowerCase(Locale.ROOT);
		for (String word : query.toLowerCase(Locale.ROOT).trim().split("\\s+"))
		{
			if (!text.contains(word))
			{
				return false;
			}
		}
		return true;
	}

	private String detail(World world)
	{
		return details.getOrDefault(world.getId(), "");
	}

	private void select(int world)
	{
		highlighted = world;
		for (int i = 0; i < model.size(); i++)
		{
			if (model.get(i).getId() == world)
			{
				list.setSelectedIndex(i);
				return;
			}
		}
		list.clearSelection();
	}

	private void scrollTo(int index)
	{
		final Rectangle cell = index < 0 ? null : list.getCellBounds(index, index);
		if (cell == null)
		{
			return;
		}
		final int visible = scroll.getViewport().getExtentSize().height;
		list.scrollRectToVisible(new Rectangle(cell.x, Math.max(0, cell.y - (visible - cell.height) / 2),
			cell.width, Math.max(visible, cell.height)));
	}

	private void hopToHighlighted()
	{
		final World world = list.getSelectedValue();
		if (world != null && canHop && world.getId() != current)
		{
			hop.accept(world);
		}
	}

	private void hopBack()
	{
		final World world = previousWorld();
		if (world != null && canHop)
		{
			hop.accept(world);
		}
	}

	// the world to hop back to, if it is listed and isn't the one logged in to
	private World previousWorld()
	{
		if (previous == current)
		{
			return null;
		}
		return worlds.stream().filter(world -> world.getId() == previous).findFirst().orElse(null);
	}

	private void updateButtons()
	{
		final World world = list.getSelectedValue();
		if (worlds.isEmpty())
		{
			hopButton.setText("The worlds haven't loaded yet");
		}
		else if (model.isEmpty())
		{
			hopButton.setText("No worlds match the search");
		}
		else if (world == null)
		{
			hopButton.setText("Pick a world to hop to");
		}
		else if (world.getId() == current)
		{
			hopButton.setText("You're on world " + world.getId());
		}
		else
		{
			hopButton.setText("Hop to world " + world.getId());
		}
		hopButton.setEnabled(world != null && canHop && world.getId() != current);

		final World back = previousWorld();
		backButton.setVisible(back != null);
		backButton.setText(back == null ? "" : "Back to world " + back.getId());
		backButton.setEnabled(back != null && canHop);
	}

	private void showMenu(MouseEvent e)
	{
		final int row = rowAt(e);
		if (!e.isPopupTrigger() || row < 0)
		{
			return;
		}
		final World world = model.get(row);
		final boolean isFavourite = favourite.test(world.getId());
		final JMenuItem item = new JMenuItem(isFavourite
			? "Remove world " + world.getId() + " from favourites"
			: "Add world " + world.getId() + " to favourites");
		item.addActionListener(event ->
		{
			setFavourite.accept(world.getId(), !isFavourite);
			refresh.run();
		});
		final JPopupMenu menu = new JPopupMenu();
		menu.add(item);
		menu.show(list, e.getX(), e.getY());
	}

	private void setHovered(int row)
	{
		if (row != hovered)
		{
			hovered = row;
			list.repaint();
		}
	}

	private int rowAt(MouseEvent e)
	{
		final int row = list.locationToIndex(e.getPoint());
		return row >= 0 && list.getCellBounds(row, row).contains(e.getPoint()) ? row : -1;
	}

	private final class Row extends JPanel implements ListCellRenderer<World>
	{
		private final JLabel title = new JLabel();
		private final JLabel players = new JLabel();
		// a long detail, such as "Current - Favourite - Members - US West - House Party", goes onto a second line
		private final JLabel detail = new Wrapping("", DETAIL_WIDTH);

		private Row()
		{
			setLayout(new BorderLayout(6, 0));

			title.setIconTextGap(6);
			players.setFont(small());
			players.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			detail.setFont(small());
			detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			// under the name rather than under the icon
			detail.setBorder(BorderFactory.createEmptyBorder(0, Icons.SIZE + 6, 0, 0));

			final JPanel top = new JPanel(new BorderLayout(6, 0));
			top.setOpaque(false);
			top.add(title, BorderLayout.CENTER);
			top.add(players, BorderLayout.EAST);
			add(top, BorderLayout.NORTH);
			add(detail, BorderLayout.SOUTH);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends World> list, World world, int index,
			boolean selected, boolean focused)
		{
			title.setText("World " + world.getId());
			title.setIcon(WorldCommands.icon(world, location.apply(world)));
			title.setForeground(selected ? ColorScheme.BRAND_ORANGE : ColorScheme.TEXT_COLOR);
			players.setText(world.getPlayers() < 0 ? "" : QuantityFormatter.formatNumber(world.getPlayers()));
			detail.setText(htmlDetails.getOrDefault(world.getId(), ""));
			setBorder(selected ? HIGHLIGHT_BORDER : ROW_BORDER);
			setBackground(selected || index == hovered ? ColorScheme.DARKER_GRAY_HOVER_COLOR
				: ColorScheme.DARKER_GRAY_COLOR);
			return this;
		}
	}
}
