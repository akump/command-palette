package com.commandpalette;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
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
		panel.show(commands, query -> TypedCommands.wikiSearch(Query.parse(query).getText()), true);
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
}
