package com.commandpalette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

/**
 * A command per world that hops to it, the way the World Hopper plugin does.
 */
@Singleton
class WorldCommands
{
	// Worlds where a slip of the finger costs something, or that an ordinary account can't use
	private static final EnumSet<WorldType> UNLISTED = EnumSet.of(WorldType.PVP, WorldType.HIGH_RISK, WorldType.DEADMAN,
		WorldType.BETA_WORLD, WorldType.NOSAVE_MODE, WorldType.TOURNAMENT, WorldType.LEGACY_ONLY, WorldType.EOC_ONLY);

	static final String KIND = "world:";

	private static final int OPEN_SWITCHER_MAX_ATTEMPTS = 3;
	private static final String BUSY_MESSAGE = "Please finish what you're doing before using the World Switcher.";

	private final Client client;
	private final ClientThread clientThread;
	private final WorldService worldService;

	// the hop in progress, which needs the game's world switcher open first
	private net.runelite.api.World target;
	private int openSwitcherAttempts;

	@Inject
	WorldCommands(Client client, ClientThread clientThread, WorldService worldService)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.worldService = worldService;
	}

	void collect(List<Command> out)
	{
		final GameState state = client.getGameState();
		final WorldResult result = worldService.getWorlds();
		if (result == null || (state != GameState.LOGGED_IN && state != GameState.LOGIN_SCREEN))
		{
			return;
		}

		final List<World> worlds = new ArrayList<>(result.getWorlds());
		worlds.sort(Comparator.comparingInt(World::getId));
		final int current = client.getWorld();
		for (World world : worlds)
		{
			if (world.getId() != current && Collections.disjoint(world.getTypes(), UNLISTED))
			{
				out.add(new Command(KIND + world.getId(), "World " + world.getId(), detail(world), Icons.GLOBE,
					() -> clientThread.invoke(() -> hop(world))));
			}
		}
	}

	static String detail(World world)
	{
		final String activity = world.getActivity();
		return "Hop - " + (world.getTypes().contains(WorldType.MEMBERS) ? "Members" : "Free")
			+ (activity == null || activity.isEmpty() || activity.equals("-") ? "" : " - " + activity);
	}

	void reset()
	{
		target = null;
		openSwitcherAttempts = 0;
	}

	private void hop(World world)
	{
		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			// on the login screen the world can just be changed
			client.changeWorld(rsWorld);
			return;
		}
		target = rsWorld;
		openSwitcherAttempts = 0;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (target == null)
		{
			return;
		}

		if (client.getWidget(InterfaceID.Worldswitcher.BUTTONS) == null)
		{
			client.openWorldHopper();
			if (++openSwitcherAttempts >= OPEN_SWITCHER_MAX_ATTEMPTS)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"Couldn't hop to world " + target.getId() + ": the world switcher wouldn't open.", null);
				reset();
			}
		}
		else
		{
			client.hopToWorld(target);
			reset();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.GAMEMESSAGE && BUSY_MESSAGE.equals(event.getMessage()))
		{
			reset();
		}
	}
}
