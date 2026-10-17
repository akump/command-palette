package com.commandpalette;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CustomCommandsTest
{
	private static final String GE = "https://prices.runescape.wiki/osrs/search?q={text}";

	private static String problem(String name, CustomCommand.Action action, String value)
	{
		return CustomCommand.problem(name, action, value, List.of("Herbs"), key -> key.equals("panel:Loot Tracker"));
	}

	@Test
	public void aCommandNeedsANameOfItsOwn()
	{
		assertTrue(problem(" ", CustomCommand.Action.COPY, "x").startsWith("Give it a name"));
		assertTrue(problem("herbs", CustomCommand.Action.COPY, "x").startsWith("There is already a command called Herbs"));
		assertTrue(problem("x".repeat(CustomCommand.MAX_NAME + 1), CustomCommand.Action.COPY, "x").startsWith("Keep"));
	}

	@Test
	public void eachActionChecksWhatItNeeds()
	{
		assertNull(problem("ge", CustomCommand.Action.LINK, GE));
		assertTrue(problem("ge", CustomCommand.Action.LINK, "prices.runescape.wiki").startsWith("A link starts"));
		assertTrue(problem("ge", CustomCommand.Action.LINK, "javascript:alert(1)").startsWith("A link starts"));
		assertNull(problem("birds", CustomCommand.Action.TIMER, "50m birdhouses"));
		assertTrue(problem("birds", CustomCommand.Action.TIMER, "birdhouses").startsWith("Start with a length"));
		assertNull(problem("loot", CustomCommand.Action.COMMAND, "panel:Loot Tracker"));
		assertTrue(problem("loot", CustomCommand.Action.COMMAND, "panel:Gone").startsWith("Pick a command"));
		assertTrue(problem("cc", CustomCommand.Action.COPY, " ").startsWith("Type the text"));
	}

	@Test
	public void whatIsTypedGoesIntoTheLinkSafely()
	{
		assertEquals("https://prices.runescape.wiki/osrs/search?q=abyssal%20whip%20%26%20more",
			CustomCommands.link(GE, "abyssal whip & more"));
		assertEquals("https://prices.runescape.wiki/osrs/search?q=", CustomCommands.link(GE, ""));
	}

	@Test
	public void theDetailSaysWhatItDoes()
	{
		assertEquals("Search prices.runescape.wiki",
			new CustomCommand("ge", CustomCommand.Action.LINK, GE).describe(key -> null));
		assertEquals("Open templeosrs.com", new CustomCommand("cl", CustomCommand.Action.LINK,
			"https://www.templeosrs.com/player/collection-log.php").describe(key -> null));
		assertEquals("Start 1h 20m timer: herb run",
			new CustomCommand("herbs", CustomCommand.Action.TIMER, "80m herb run").describe(key -> null));
		assertEquals("Run Loot Tracker", new CustomCommand("loot", CustomCommand.Action.COMMAND, "panel:Loot Tracker")
			.describe(Map.of("panel:Loot Tracker", "Loot Tracker")::get));
		assertEquals("Command not available now",
			new CustomCommand("loot", CustomCommand.Action.COMMAND, "panel:Gone").describe(key -> null));
	}

	@Test
	public void theyAreSavedAndReadBack()
	{
		final Gson gson = new Gson();
		final List<CustomCommand> commands = List.of(new CustomCommand("ge", CustomCommand.Action.LINK, GE),
			new CustomCommand("herbs", CustomCommand.Action.TIMER, "80m herb run"));
		assertEquals(commands, CustomCommands.parse(gson, CustomCommands.toJson(gson, commands)));
		// what can't be read is left out rather than losing the rest
		assertEquals(List.of(), CustomCommands.parse(gson, "not json"));
		assertEquals(List.of(), CustomCommands.parse(gson, null));
	}

	@Test
	public void aCustomCommandCantRunAnother()
	{
		assertTrue(CustomCommands.canRun(new Command("panel:Notes", "Notes", "", null, () -> {})));
		assertEquals(false, CustomCommands.canRun(new Command("custom:ge", "ge", "", null, () -> {})));
		assertEquals(false, CustomCommands.canRun(new Command("Help", "", null, () -> {})));
	}

	@Test
	public void aLinkTakingTextIsOfferedForWhatIsTypedAfterItsName()
	{
		final List<CustomCommand> saved = new ArrayList<>(List.of(new CustomCommand("ge", CustomCommand.Action.LINK, GE),
			new CustomCommand("cl", CustomCommand.Action.LINK, "https://example.com/log")));
		final CustomCommands custom = new CustomCommands(null, new Gson(), null)
		{
			@Override
			List<CustomCommand> load()
			{
				return saved;
			}
		};
		assertEquals(List.of("ge: abyssal whip"), titles(custom.forQuery("GE abyssal whip ", key -> null)));
		// not without the text, and not for a link that takes none
		assertEquals(List.of(), titles(custom.forQuery("ge", key -> null)));
		assertEquals(List.of(), titles(custom.forQuery("cl something", key -> null)));
	}

	private static List<String> titles(List<Command> commands)
	{
		final List<String> titles = new ArrayList<>();
		commands.forEach(c -> titles.add(c.getTitle()));
		return titles;
	}
}
