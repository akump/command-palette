package com.commandpalette;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;

/**
 * Hops the way the World Hopper plugin does. Only {@link WorldPanel} calls this, from a click: a plugin may only hop
 * when the user clicks for it in a side panel, so neither the palette nor a key press may.
 */
@Singleton
class WorldHopper
{
	private static final int OPEN_SWITCHER_MAX_ATTEMPTS = 3;
	private static final String BUSY_MESSAGE = "Please finish what you're doing before using the World Switcher.";

	private final Client client;
	private final ClientThread clientThread;

	// the hop in progress, which needs the game's world switcher open first
	private net.runelite.api.World target;
	private int openSwitcherAttempts;

	// the world logged in to, and the one before it, or -1; for hopping back
	private volatile int currentWorld = -1;
	private volatile int previousWorld = -1;

	@Inject
	WorldHopper(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	void hop(World world)
	{
		clientThread.invoke(() ->
		{
			final net.runelite.api.World rsWorld = client.createWorld();
			rsWorld.setActivity(world.getActivity());
			rsWorld.setAddress(world.getAddress());
			rsWorld.setId(world.getId());
			rsWorld.setPlayerCount(world.getPlayers());
			rsWorld.setLocation(world.getRegion().getId());
			rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

			if (client.getGameState() == GameState.LOGIN_SCREEN)
			{
				// on the login screen the world can just be changed
				client.changeWorld(rsWorld);
				return;
			}
			target = rsWorld;
			openSwitcherAttempts = 0;
		});
	}

	/**
	 * The world logged in to before this one, or -1.
	 */
	int getPreviousWorld()
	{
		return previousWorld;
	}

	void reset()
	{
		target = null;
		openSwitcherAttempts = 0;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			previousWorld = previous(previousWorld, currentWorld, client.getWorld());
			currentWorld = client.getWorld();
		}
	}

	/**
	 * The world to hop back to once logged in to the next one: the last one, unless it is the same world again.
	 */
	static int previous(int previous, int last, int next)
	{
		return last == -1 || last == next ? previous : last;
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
