package com.commandpalette;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;
import static org.junit.Assert.assertEquals;
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
	public void detailSaysWhatKindOfWorldItIs()
	{
		assertEquals("Free", WorldCommands.detail(world(301, "-"), false, false));
		assertEquals("Members - Wintertodt",
			WorldCommands.detail(world(309, "Wintertodt", WorldType.MEMBERS), false, false));
		assertEquals("Favourite - Members", WorldCommands.detail(world(302, "", WorldType.MEMBERS), true, false));
		assertEquals("Current - Favourite - Members",
			WorldCommands.detail(world(302, "", WorldType.MEMBERS), true, true));
	}
}
