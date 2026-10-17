package com.commandpalette;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JTextField;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PalettePanelTest
{
	private final List<String> events = new ArrayList<>();
	private final PalettePanel panel = new PalettePanel(new PalettePanel.Listener()
	{
		@Override
		public void run(Command command)
		{
			events.add("run " + command.getTitle());
		}

		@Override
		public void cancel()
		{
			events.add("cancel");
		}

		@Override
		public void resized()
		{
		}
	});

	private void show(int count)
	{
		final List<Command> commands = new ArrayList<>();
		for (int i = 1; i <= count; i++)
		{
			commands.add(new Command("Panel " + i, "Open panel", null, () -> {}));
		}
		panel.show(List.of(), List.of(), commands, query -> TypedCommands.wikiSearch(Query.parse(query).getText()), true);
	}

	// Key events dispatched to a field that isn't on screen go nowhere, so hand them to its listeners directly

	private void press(int keyCode, int modifiers)
	{
		final JTextField search = panel.getSearch();
		final KeyEvent event = new KeyEvent(search, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
		for (KeyListener listener : search.getKeyListeners())
		{
			listener.keyPressed(event);
		}
	}

	@Test
	public void enterRunsTheFirstCommandByDefault()
	{
		show(3);
		press(KeyEvent.VK_ENTER, 0);
		assertEquals(List.of("run Panel 1"), events);
	}

	@Test
	public void arrowsAndTabMoveAndWrap()
	{
		show(3);
		press(KeyEvent.VK_DOWN, 0);
		press(KeyEvent.VK_TAB, 0);
		press(KeyEvent.VK_ENTER, 0);
		press(KeyEvent.VK_DOWN, 0);
		press(KeyEvent.VK_ENTER, 0);
		press(KeyEvent.VK_UP, 0);
		press(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK);
		press(KeyEvent.VK_ENTER, 0);
		assertEquals(List.of("run Panel 3", "run Panel 1", "run Panel 2"), events);
	}

	@Test
	public void pagingStopsAtTheEnds()
	{
		show(30);
		press(KeyEvent.VK_PAGE_DOWN, 0);
		press(KeyEvent.VK_ENTER, 0);
		press(KeyEvent.VK_PAGE_DOWN, 0);
		press(KeyEvent.VK_PAGE_DOWN, 0);
		press(KeyEvent.VK_ENTER, 0);
		press(KeyEvent.VK_PAGE_UP, 0);
		press(KeyEvent.VK_PAGE_UP, 0);
		press(KeyEvent.VK_PAGE_UP, 0);
		press(KeyEvent.VK_ENTER, 0);
		assertEquals(List.of("run Panel 13", "run Panel 30", "run Panel 1"), events);
	}

	@Test
	public void escapeCancels()
	{
		show(3);
		press(KeyEvent.VK_ESCAPE, 0);
		assertEquals(List.of("cancel"), events);
	}

	@Test
	public void wikiSearchComesAfterTheMatches()
	{
		show(3);
		panel.getSearch().setText(" panel 2 ");
		press(KeyEvent.VK_ENTER, 0);
		press(KeyEvent.VK_DOWN, 0);
		press(KeyEvent.VK_ENTER, 0);
		assertEquals(List.of("run Panel 2", "run Search the wiki for \"panel 2\""), events);
	}

	@Test
	public void wikiSearchIsAllThatIsLeftWhenNothingMatches()
	{
		show(3);
		panel.getSearch().setText("abyssal whip");
		press(KeyEvent.VK_ENTER, 0);
		assertEquals(List.of("run Search the wiki for \"abyssal whip\""), events);
	}

	@Test
	public void showingAgainClearsTheSearch()
	{
		show(3);
		panel.getSearch().setText("panel 3");
		show(3);
		press(KeyEvent.VK_ENTER, 0);
		assertEquals(List.of("run Panel 1"), events);
	}

	@Test
	public void recentCommandsComeFirstAndASearchRanksThemWithTheRest()
	{
		final Command recent = new Command("Panel 9", "Open panel", null, () -> {});
		final Command rest = new Command("Panel 1", "Open panel", null, () -> {});
		panel.show(List.of(recent), List.of(), List.of(rest), query -> List.of(), true);
		press(KeyEvent.VK_ENTER, 0);
		panel.getSearch().setText("panel 1");
		press(KeyEvent.VK_ENTER, 0);
		assertEquals(List.of("run Panel 9", "run Panel 1"), events);
	}

	@Test
	public void pinnedCommandsComeAfterTheRecentAndBeforeTheRest()
	{
		final Command recent = new Command("Panel 9", "Open panel", null, () -> {});
		final Command pinned = new Command("Panel 5", "Open panel", null, () -> {});
		final Command rest = new Command("Panel 1", "Open panel", null, () -> {});
		panel.show(List.of(recent), List.of(pinned), List.of(rest), query -> List.of(), true);
		press(KeyEvent.VK_DOWN, 0);
		press(KeyEvent.VK_ENTER, 0);
		assertEquals(List.of("run Panel 5"), events);
	}

	@Test
	public void eachSectionAfterTheFirstStartsWithALine()
	{
		// recent, pinned and the rest
		assertEquals(Set.of(2, 5), PalettePanel.sectionStarts(List.of(2, 3, 10)));
		// no recent: the pinned come first, with no line above them
		assertEquals(Set.of(3), PalettePanel.sectionStarts(List.of(0, 3, 10)));
		// nothing pinned
		assertEquals(Set.of(2), PalettePanel.sectionStarts(List.of(2, 0, 10)));
		// only the rest
		assertEquals(Set.of(), PalettePanel.sectionStarts(List.of(0, 0, 10)));
	}

	// the renderer, set up for the command, laid out as wide as a row of the palette
	private java.awt.Container row(Command command)
	{
		@SuppressWarnings("unchecked")
		final javax.swing.JList<Command> list = (javax.swing.JList<Command>) find(panel, javax.swing.JList.class);
		final java.awt.Container row = (java.awt.Container) list.getCellRenderer()
			.getListCellRendererComponent(list, command, 0, false, false);
		row.setSize(list.getFixedCellWidth(), list.getFixedCellHeight());
		row.doLayout();
		return row;
	}

	private static java.awt.Component find(java.awt.Container container, Class<?> type)
	{
		for (java.awt.Component child : container.getComponents())
		{
			if (type.isInstance(child))
			{
				return child;
			}
			if (child instanceof java.awt.Container)
			{
				final java.awt.Component found = find((java.awt.Container) child, type);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	@Test
	public void aRowShowsItsNameAndItsDetailInFullWhenTheyFit()
	{
		show(1);
		final java.awt.Component[] labels = row(new Command("Configuration", "Open panel", null, () -> {})).getComponents();
		// the name fills what the detail leaves
		org.junit.Assert.assertTrue(labels[0].getWidth() >= labels[0].getPreferredSize().width);
		assertEquals(labels[1].getPreferredSize().width, labels[1].getWidth());
		org.junit.Assert.assertTrue(labels[1].getWidth() > 0);
	}

	@Test
	public void aLongDetailIsCutShortForTheName()
	{
		show(1);
		final java.awt.Component[] labels = row(new Command("World 330",
			"Current - Favourite - Members - US West - House Party, Gilded Altar, and more besides", null, () -> {}))
			.getComponents();
		// the name is all there, and the detail takes the rest
		org.junit.Assert.assertTrue(labels[0].getWidth() >= labels[0].getPreferredSize().width);
		org.junit.Assert.assertTrue(labels[1].getWidth() > 0);
		org.junit.Assert.assertTrue(labels[1].getX() + labels[1].getWidth() <= 320);
	}
}
