package com.commandpalette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.IntPredicate;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.WorldService;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

/**
 * A command per world, to look one up by its number or its activity. The worlds made favourites in the World Hopper
 * plugin's panel come first. Running one doesn't hop, which a plugin like this one isn't allowed to do: it opens
 * that plugin's panel, where the world can be clicked.
 */
class WorldCommands
{
	// Worlds that an ordinary account doesn't use, or can't
	private static final EnumSet<WorldType> UNLISTED = EnumSet.of(WorldType.PVP, WorldType.HIGH_RISK, WorldType.DEADMAN,
		WorldType.BETA_WORLD, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT, WorldType.LEGACY_ONLY, WorldType.EOC_ONLY);

	static final String KIND = "world:";
	// the name of the World Hopper plugin's side panel
	static final String SWITCHER_PANEL = "World Switcher";

	// where the World Hopper plugin keeps its favourites, one setting per world
	private static final String WORLD_HOPPER_GROUP = "worldhopper";
	private static final String FAVOURITE_PREFIX = "favorite_";

	private final Client client;
	private final WorldService worldService;
	private final ConfigManager configManager;

	@Inject
	WorldCommands(Client client, WorldService worldService, ConfigManager configManager)
	{
		this.client = client;
		this.worldService = worldService;
		this.configManager = configManager;
	}

	/**
	 * @param openSwitcher opens the World Hopper plugin's panel, if it has one
	 */
	void collect(List<Command> out, Runnable openSwitcher)
	{
		final WorldResult result = worldService.getWorlds();
		if (result == null)
		{
			return;
		}

		final int current = client.getGameState() == GameState.LOGGED_IN ? client.getWorld() : -1;
		for (World world : favouritesFirst(result.getWorlds(), this::isFavourite))
		{
			if (Collections.disjoint(world.getTypes(), UNLISTED))
			{
				final boolean favourite = isFavourite(world.getId());
				out.add(new Command(KIND + world.getId(), "World " + world.getId(),
					detail(world, favourite, world.getId() == current), favourite ? Icons.STAR : Icons.GLOBE,
					openSwitcher));
			}
		}
	}

	private boolean isFavourite(int world)
	{
		return Boolean.TRUE.equals(
			configManager.getConfiguration(WORLD_HOPPER_GROUP, FAVOURITE_PREFIX + world, Boolean.class));
	}

	/**
	 * The worlds in order of their numbers, the favourites before the rest.
	 */
	static List<World> favouritesFirst(List<World> worlds, IntPredicate favourite)
	{
		final List<World> ordered = new ArrayList<>(worlds);
		ordered.sort(Comparator.comparing((World world) -> !favourite.test(world.getId()))
			.thenComparingInt(World::getId));
		return ordered;
	}

	/**
	 * "Favourite - Members - Wintertodt"
	 */
	static String detail(World world, boolean favourite, boolean current)
	{
		final String activity = world.getActivity();
		return (current ? "Current - " : "") + (favourite ? "Favourite - " : "")
			+ (world.getTypes().contains(WorldType.MEMBERS) ? "Members" : "Free")
			+ (activity == null || activity.isEmpty() || activity.equals("-") ? "" : " - " + activity);
	}
}
