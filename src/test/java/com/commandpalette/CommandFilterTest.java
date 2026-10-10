package com.commandpalette;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CommandFilterTest
{
	private static final List<Command> COMMANDS = Arrays.asList(
		command("Configuration", "Open panel"),
		command("Loot Tracker", "Open panel"),
		command("XP Tracker", "Open panel"),
		command("Notes", "Open panel"),
		command("Loot Tracker", "Turn plugin off"),
		command("Tracker Extras", "Turn plugin on"));

	private static Command command(String title, String detail)
	{
		return new Command(title, detail, null, () -> {});
	}

	private static List<String> filter(String query)
	{
		return CommandFilter.filter(COMMANDS, query).stream()
			.map(c -> c.getTitle() + " / " + c.getDetail())
			.collect(Collectors.toList());
	}

	@Test
	public void emptyQueryKeepsEverythingInOrder()
	{
		assertEquals(COMMANDS, CommandFilter.filter(COMMANDS, "  "));
	}

	@Test
	public void matchesAnywhereIgnoringCase()
	{
		assertEquals(Arrays.asList("Notes / Open panel"), filter("OTE"));
	}

	@Test
	public void titlePrefixThenWordPrefixThenTheRest()
	{
		assertEquals(Arrays.asList(
			"Tracker Extras / Turn plugin on",
			"Loot Tracker / Open panel",
			"XP Tracker / Open panel",
			"Loot Tracker / Turn plugin off"), filter("tracker"));
	}

	@Test
	public void detailIsSearchable()
	{
		assertEquals(Arrays.asList("Loot Tracker / Turn plugin off", "Tracker Extras / Turn plugin on"), filter("plugin"));
	}

	@Test
	public void everyWordMustMatch()
	{
		assertEquals(Arrays.asList("Loot Tracker / Turn plugin off"), filter("loot off"));
		assertEquals(Arrays.asList(), filter("loot notes"));
	}

	private static List<String> titles(String query)
	{
		final List<Command> commands = Arrays.asList(
			new Command("panel:Loot Tracker", "Loot Tracker", "Open panel", null, () -> {}),
			new Command("plugin:Loot Tracker", "Loot Tracker", "Turn plugin off", null, () -> {}),
			new Command("plugin:World Hopper", "World Hopper", "Turn plugin off", null, () -> {}),
			new Command("world:302", "World 302", "Members", null, () -> {}),
			new Command("world:330", "World 330", "Members - House Party", null, () -> {}));
		return CommandFilter.filter(commands, query).stream()
			.map(c -> c.getTitle() + " / " + c.getDetail())
			.collect(Collectors.toList());
	}

	@Test
	public void aPrefixNarrowsToOneKind()
	{
		assertEquals(Arrays.asList("Loot Tracker / Open panel"), titles("#"));
		assertEquals(Arrays.asList("Loot Tracker / Open panel"), titles("# loot"));
		assertEquals(Arrays.asList(), titles("#world"));
		assertEquals(Arrays.asList("Loot Tracker / Turn plugin off", "World Hopper / Turn plugin off"), titles(">"));
		assertEquals(Arrays.asList("Loot Tracker / Turn plugin off"), titles("> loot"));
		assertEquals(Arrays.asList("World 302 / Members", "World 330 / Members - House Party"), titles("@"));
		assertEquals(Arrays.asList("World 330 / Members - House Party"), titles("@party"));
		assertEquals(Arrays.asList("World 302 / Members"), titles(" @ 302"));
	}

	@Test
	public void theWikiPrefixFindsNoCommands()
	{
		assertEquals(Arrays.asList(), titles("?loot"));
	}

	@Test
	public void withoutAPrefixEveryKindIsSearched()
	{
		assertEquals(Arrays.asList("World Hopper / Turn plugin off", "World 302 / Members",
			"World 330 / Members - House Party"), titles("world"));
	}
}
