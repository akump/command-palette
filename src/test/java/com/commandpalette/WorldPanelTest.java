package com.commandpalette;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

public class WorldPanelTest
{
	// RuneLite's search field may only be changed on the event thread, as it is in the client
	@Rule
	public final TestRule onEventThread = (base, description) -> new Statement()
	{
		@Override
		public void evaluate() throws Throwable
		{
			final Throwable[] thrown = new Throwable[1];
			SwingUtilities.invokeAndWait(() ->
			{
				try
				{
					base.evaluate();
				}
				catch (Throwable t)
				{
					thrown[0] = t;
				}
			});
			if (thrown[0] != null)
			{
				throw thrown[0];
			}
		}
	};

	private final List<World> worlds = new ArrayList<>(List.of(world(301), world(302), world(330)));
	private int current = -1;
	private int previous = -1;
	private boolean canHop = true;
	private final List<Integer> favourites = new ArrayList<>(List.of(330));
	private final List<Integer> hops = new ArrayList<>();
	private final WorldPanel[] holder = new WorldPanel[1];
	private final WorldPanel panel = new WorldPanel(
		() -> holder[0].show(worlds, world -> null, favourites::contains, current, previous, canHop),
		world -> hops.add(world.getId()),
		(id, favourite) -> favourites.add(id));

	{
		holder[0] = panel;
	}

	private static World world(int id)
	{
		return world(id, 100);
	}

	private static World world(int id, int players, WorldType... types)
	{
		final EnumSet<WorldType> set = EnumSet.noneOf(WorldType.class);
		set.addAll(List.of(types));
		return World.builder().id(id).activity("-").types(set).players(players).build();
	}

	private void doubleClick(int row)
	{
		final JList<World> list = panel.getList();
		list.setSize(200, 1000);
		list.doLayout();
		final java.awt.Rectangle cell = list.getCellBounds(row, row);
		final MouseEvent event = new MouseEvent(list, MouseEvent.MOUSE_CLICKED, 0, 0,
			cell.x + 1, cell.y + 1, 2, false, MouseEvent.BUTTON1);
		for (MouseListener listener : list.getMouseListeners())
		{
			listener.mouseClicked(event);
		}
	}

	@Test
	public void highlightingAWorldSelectsItAndOffersTheHop()
	{
		panel.highlight(302);
		assertEquals(302, panel.getList().getSelectedValue().getId());
		assertEquals("Hop to world 302", panel.getHopButton().getText());
		assertTrue(panel.getHopButton().isEnabled());
		assertTrue(hops.isEmpty());

		panel.getHopButton().doClick(0);
		assertEquals(List.of(302), hops);
	}

	@Test
	public void theHighlightOutlastsARefresh()
	{
		panel.highlight(330);
		worlds.add(0, world(300));
		panel.refresh();
		assertEquals(330, panel.getList().getSelectedValue().getId());
	}

	@Test
	public void doubleClickingAWorldHopsToIt()
	{
		panel.refresh();
		// the favourite first, then by number
		assertEquals(List.of(330, 301, 302), listed());
		panel.getList().setSelectedIndex(2);
		doubleClick(2);
		assertEquals(List.of(302), hops);
	}

	@Test
	public void theCurrentWorldCantBeHoppedTo()
	{
		current = 302;
		panel.highlight(302);
		assertEquals("You're on world 302", panel.getHopButton().getText());
		assertFalse(panel.getHopButton().isEnabled());
		doubleClick(1);
		assertTrue(hops.isEmpty());
	}

	@Test
	public void nothingHopsWhileTheGameCant()
	{
		canHop = false;
		panel.highlight(301);
		assertFalse(panel.getHopButton().isEnabled());
		doubleClick(0);
		assertTrue(hops.isEmpty());
	}

	private List<Integer> listed()
	{
		final List<Integer> ids = new ArrayList<>();
		for (int i = 0; i < panel.getList().getModel().getSize(); i++)
		{
			ids.add(panel.getList().getModel().getElementAt(i).getId());
		}
		return ids;
	}

	@Test
	public void theSearchMatchesEveryWordAgainstTheNameAndDetail()
	{
		final World wintertodt = World.builder().id(309).activity("Wintertodt")
			.types(EnumSet.of(WorldType.MEMBERS)).build();
		assertTrue(WorldPanel.matches(wintertodt, "Members - Wintertodt", ""));
		assertTrue(WorldPanel.matches(wintertodt, "Members - Wintertodt", "309"));
		assertTrue(WorldPanel.matches(wintertodt, "Members - Wintertodt", " WINT  members "));
		assertFalse(WorldPanel.matches(wintertodt, "Members - Wintertodt", "wint free"));
	}

	@Test
	public void searchingNarrowsTheList()
	{
		panel.refresh();
		panel.getSearch().setText("world 302");
		assertEquals(List.of(302), listed());
		panel.getSearch().setText("favourite");
		assertEquals(List.of(330), listed());
		panel.getSearch().setText("nothing");
		assertEquals(List.of(), listed());
		assertEquals("No worlds match the search", panel.getHopButton().getText());
	}

	@Test
	public void aWorldHiddenBySearchIsHighlightedAgainOnceItShows()
	{
		panel.highlight(302);
		panel.getSearch().setText("330");
		assertFalse(panel.getHopButton().isEnabled());
		panel.getSearch().setText("");
		assertEquals(302, panel.getList().getSelectedValue().getId());
	}

	@Test
	public void highlightingFromThePaletteClearsTheSearch()
	{
		panel.refresh();
		panel.getSearch().setText("330");
		panel.highlight(301);
		assertEquals("", panel.getSearch().getText());
		assertEquals(301, panel.getList().getSelectedValue().getId());
	}

	@Test
	public void theMembershipChoiceNarrowsTheList()
	{
		worlds.add(world(310, 50, WorldType.MEMBERS));
		panel.refresh();
		panel.getMembership().setSelectedItem(WorldPanel.Membership.MEMBERS);
		assertEquals(List.of(310), listed());
		panel.getMembership().setSelectedItem(WorldPanel.Membership.FREE);
		assertEquals(List.of(330, 301, 302), listed());
	}

	@Test
	public void sortingByPlayersKeepsTheFavouritesFirst()
	{
		final List<World> unsorted = List.of(world(301, 10), world(302, 900), world(330, 5), world(303, 900));
		assertEquals(List.of(330, 302, 303, 301), WorldPanel.ordered(unsorted, id -> id == 330,
			WorldPanel.Sort.PLAYERS).stream().map(World::getId).collect(Collectors.toList()));
		assertEquals(List.of(330, 301, 302, 303), WorldPanel.ordered(unsorted, id -> id == 330,
			WorldPanel.Sort.NUMBER).stream().map(World::getId).collect(Collectors.toList()));
	}

	@Test
	public void highlightingFromThePaletteShowsEveryWorldAgain()
	{
		panel.getMembership().setSelectedItem(WorldPanel.Membership.MEMBERS);
		panel.highlight(301);
		assertEquals(WorldPanel.Membership.ALL, panel.getMembership().getSelectedItem());
		assertEquals(301, panel.getList().getSelectedValue().getId());
	}

	@Test
	public void theBackButtonHopsToThePreviousWorld()
	{
		current = 301;
		previous = 302;
		panel.refresh();
		assertTrue(panel.getBackButton().isVisible());
		assertEquals("Back to world 302", panel.getBackButton().getText());
		panel.getBackButton().doClick(0);
		assertEquals(List.of(302), hops);
	}

	@Test
	public void theBackButtonIsHiddenWithoutAWorldToGoBackTo()
	{
		current = 301;
		panel.refresh();
		assertFalse(panel.getBackButton().isVisible());

		// one that isn't listed, such as a PvP world
		previous = 325;
		panel.refresh();
		assertFalse(panel.getBackButton().isVisible());
	}

	@Test
	public void withoutAHighlightTheButtonAsksForOne()
	{
		panel.refresh();
		assertEquals("Pick a world to hop to", panel.getHopButton().getText());
		assertFalse(panel.getHopButton().isEnabled());

		worlds.clear();
		panel.refresh();
		assertEquals("The worlds haven't loaded yet", panel.getHopButton().getText());
	}

	@Test
	public void aLongDetailWrapsRatherThanWideningTheList()
	{
		worlds.set(0, World.builder().id(301).activity("House Party, Gilded Altar").players(1043)
			.types(EnumSet.of(WorldType.MEMBERS)).build());
		current = 301;
		panel.refresh();
		assertTrue(panel.getList().getScrollableTracksViewportWidth());

		// the renderer is one component for every row, so each is measured as soon as it is set up
		final int shortHeight = panel.getList().getCellRenderer()
			.getListCellRendererComponent(panel.getList(), worlds.get(1), 2, false, false).getPreferredSize().height;
		final java.awt.Dimension size = panel.getList().getCellRenderer()
			.getListCellRendererComponent(panel.getList(), worlds.get(0), 1, true, false).getPreferredSize();
		assertTrue("as wide as " + size.width, size.width <= SideTab.TEXT_WIDTH);
		// "Current - Favourite - Members - House Party, Gilded Altar" takes a second line
		assertTrue(size.height + " against " + shortHeight, size.height > shortHeight);
	}
}
