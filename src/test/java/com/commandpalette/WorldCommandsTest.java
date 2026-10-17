package com.commandpalette;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class WorldCommandsTest
{
	private static World world(int id, String activity, WorldType... types)
	{
		final EnumSet<WorldType> set = EnumSet.noneOf(WorldType.class);
		set.addAll(List.of(types));
		return World.builder().id(id).activity(activity).types(set).build();
	}

	@Test
	public void favouritesComeFirstAndOtherwiseInOrderOfNumber()
	{
		final List<World> worlds = List.of(world(330, "-"), world(302, "-"), world(416, "-"), world(301, "-"));
		assertEquals(List.of(330, 416, 301, 302),
			WorldCommands.favouritesFirst(worlds, id -> id == 416 || id == 330).stream()
				.map(World::getId).collect(Collectors.toList()));
	}

	@Test
	public void unlistedWorldsAreLeftOut()
	{
		final List<World> worlds = List.of(world(301, "-"), world(325, "PvP", WorldType.PVP),
			world(345, "Deadman", WorldType.DEADMAN), world(302, "-"));
		assertEquals(List.of(302, 301),
			WorldCommands.listed(worlds, id -> id == 302).stream().map(World::getId).collect(Collectors.toList()));
	}

	@Test
	public void detailSaysWhatKindOfWorldItIs()
	{
		assertEquals("Free", WorldCommands.detail(world(301, "-"), null, false, false));
		assertEquals("Members - Wintertodt",
			WorldCommands.detail(world(309, "Wintertodt", WorldType.MEMBERS), null, false, false));
		assertEquals("Favourite - Members", WorldCommands.detail(world(302, "", WorldType.MEMBERS), null, true, false));
		assertEquals("Current - Favourite - Members",
			WorldCommands.detail(world(302, "", WorldType.MEMBERS), null, true, true));
	}

	@Test
	public void aWorldsIconIsForItsRegionAndMembership()
	{
		final World members = World.builder().id(302).region(WorldRegion.UNITED_KINGDOM)
			.types(EnumSet.of(WorldType.MEMBERS)).build();
		final World free = World.builder().id(301).region(WorldRegion.UNITED_KINGDOM)
			.types(EnumSet.noneOf(WorldType.class)).build();
		assertSame(WorldIcons.of(WorldLocation.UK, true), WorldCommands.icon(members, WorldLocation.UK));
		assertSame(WorldIcons.of(WorldLocation.UK, false), WorldCommands.icon(free, WorldLocation.UK));
		assertNotSame(WorldCommands.icon(members, WorldLocation.UK), WorldCommands.icon(free, WorldLocation.UK));
		assertNotSame(WorldIcons.of(WorldLocation.US_EAST, true), WorldIcons.of(WorldLocation.US_WEST, true));
		assertEquals(Icons.SIZE, WorldIcons.of(null, true).getIconWidth());
	}

	@Test
	public void theDetailSaysWhereTheWorldIs()
	{
		assertEquals("Members - US West - Wintertodt",
			WorldCommands.detail(world(309, "Wintertodt", WorldType.MEMBERS), WorldLocation.US_WEST, false, false));
		assertEquals("Free - Germany", WorldCommands.detail(world(301, "-"), WorldLocation.GERMANY, false, false));
	}

	@Test
	public void aUsWorldsCoastComesFromTheGamesLocationNumber()
	{
		final WorldRegion us = WorldRegion.UNITED_STATES_OF_AMERICA;
		assertEquals(WorldLocation.US_WEST, WorldLocation.of(us, WorldLocation.US_WEST_LOCATION));
		assertEquals(WorldLocation.US_EAST, WorldLocation.of(us, WorldLocation.US_EAST_LOCATION));
		// not read yet, or a number it doesn't know
		assertEquals(WorldLocation.US, WorldLocation.of(us, null));
		assertEquals(WorldLocation.US, WorldLocation.of(us, 0));
		assertEquals(WorldLocation.UK, WorldLocation.of(WorldRegion.UNITED_KINGDOM, WorldLocation.US_WEST_LOCATION));
		assertEquals(null, WorldLocation.of(null, null));
	}
}
