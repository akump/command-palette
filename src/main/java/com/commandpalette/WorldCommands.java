package com.commandpalette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Icon;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.WorldService;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

/**
 * A command per world, to look one up by its number or its activity. The worlds made favourites in the World Hopper
 * plugin's panel come first. Running one doesn't hop, which a plugin may only do from a click in a side panel: it
 * picks the world out in the side panel's {@link WorldPanel}, where the click is left to the user.
 */
@Singleton
class WorldCommands
{
	// Worlds that an ordinary account doesn't use, or can't
	private static final EnumSet<WorldType> UNLISTED = EnumSet.of(WorldType.PVP, WorldType.HIGH_RISK, WorldType.DEADMAN,
		WorldType.BETA_WORLD, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT, WorldType.LEGACY_ONLY, WorldType.EOC_ONLY);

	static final String KIND = "world:";
	// the name of the World Hopper plugin's side panel, opened instead while this plugin's side panel is off
	static final String SWITCHER_PANEL = "World Switcher";

	// where the World Hopper plugin keeps its favourites, one setting per world
	private static final String WORLD_HOPPER_GROUP = "worldhopper";
	private static final String FAVOURITE_PREFIX = "favorite_";

	private final Client client;
	private final WorldService worldService;
	private final ConfigManager configManager;
	private final WorldHopper worldHopper;
	private final ClientThread clientThread;

	// the game's location number for each world, which is how a US world's coast is known; read from its cache
	private final Map<Integer, Integer> locations = new ConcurrentHashMap<>();

	@Inject
	WorldCommands(Client client, WorldService worldService, ConfigManager configManager, WorldHopper worldHopper,
		ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.worldService = worldService;
		this.configManager = configManager;
		this.worldHopper = worldHopper;
	}

	/**
	 * @param show picks the world of that number out in the world panel
	 */
	void collect(List<Command> out, IntConsumer show)
	{
		final int current = currentWorld();
		final List<World> worlds = listed();
		final int previous = worldHopper.getPreviousWorld();
		if (previous != current && worlds.stream().anyMatch(world -> world.getId() == previous))
		{
			out.add(new Command(KIND + "previous", "Previous world", "World " + previous, Icons.RECENT,
				() -> show.accept(previous)));
		}
		for (World world : worlds)
		{
			final boolean favourite = isFavourite(world.getId());
			final WorldLocation location = location(world);
			out.add(new Command(KIND + world.getId(), "World " + world.getId(),
				detail(world, location, favourite, world.getId() == current), icon(world, location),
				() -> show.accept(world.getId())));
		}
	}

	/**
	 * Fills the panel with the worlds as they are now.
	 */
	void refresh(WorldPanel panel)
	{
		final GameState state = client.getGameState();
		panel.show(listed(), this::location, this::isFavourite, currentWorld(), worldHopper.getPreviousWorld(),
			state == GameState.LOGGED_IN || state == GameState.LOGIN_SCREEN);
	}

	/**
	 * Reads where each world is from the game's cache, once it has loaded, and then runs the callback on the client
	 * thread. Until then the US worlds are known only as US.
	 */
	void loadLocations(Runnable onLoaded)
	{
		clientThread.invoke(() ->
		{
			final WorldResult result = worldService.getWorlds();
			if (result == null || client.getGameState().getState() < GameState.LOGIN_SCREEN.getState())
			{
				// tried again on the next tick
				return false;
			}
			final EnumComposition enumeration = client.getEnum(EnumID.WORLD_LOCATIONS);
			for (World world : result.getWorlds())
			{
				locations.put(world.getId(), enumeration.getIntValue(world.getId()));
			}
			onLoaded.run();
			return true;
		});
	}

	WorldLocation location(World world)
	{
		return WorldLocation.of(world.getRegion(), locations.get(world.getId()));
	}

	static Icon icon(World world, WorldLocation location)
	{
		return WorldIcons.of(location, world.getTypes().contains(WorldType.MEMBERS));
	}

	/**
	 * Adds the world to the World Hopper plugin's favourites, or takes it out, the way that plugin does.
	 */
	void setFavourite(int world, boolean favourite)
	{
		if (favourite)
		{
			configManager.setConfiguration(WORLD_HOPPER_GROUP, FAVOURITE_PREFIX + world, true);
		}
		else
		{
			configManager.unsetConfiguration(WORLD_HOPPER_GROUP, FAVOURITE_PREFIX + world);
		}
	}

	/**
	 * The worlds to list, favourites first, or none before the client has fetched them.
	 */
	List<World> listed()
	{
		final WorldResult result = worldService.getWorlds();
		return result == null ? Collections.emptyList() : listed(result.getWorlds(), this::isFavourite);
	}

	static List<World> listed(List<World> worlds, IntPredicate favourite)
	{
		final List<World> listed = new ArrayList<>();
		for (World world : favouritesFirst(worlds, favourite))
		{
			if (Collections.disjoint(world.getTypes(), UNLISTED))
			{
				listed.add(world);
			}
		}
		return listed;
	}

	// the world logged in to, or -1
	private int currentWorld()
	{
		return client.getGameState() == GameState.LOGGED_IN ? client.getWorld() : -1;
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
	 * "Favourite - Members - US West - Wintertodt"
	 *
	 * @param location where the world is, or null if unknown
	 */
	static String detail(World world, WorldLocation location, boolean favourite, boolean current)
	{
		final String activity = world.getActivity();
		return (current ? "Current - " : "") + (favourite ? "Favourite - " : "")
			+ (world.getTypes().contains(WorldType.MEMBERS) ? "Members" : "Free")
			+ (location == null ? "" : " - " + location.getName())
			+ (activity == null || activity.isEmpty() || activity.equals("-") ? "" : " - " + activity);
	}
}
