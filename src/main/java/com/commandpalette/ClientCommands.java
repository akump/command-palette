package com.commandpalette;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.LinkBrowser;

/**
 * Odds and ends of the client and the logged in player: a screenshot, things worth copying, and the
 * player's pages on the hiscores and tracking sites.
 */
class ClientCommands
{
	private static final String HISCORES = "https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal?user1=";
	private static final String GROUP_HISCORES = "https://secure.runescape.com/m=hiscore_oldschool_ironman/group-ironman/view-group?name=";
	private static final String WISE_OLD_MAN = "https://wiseoldman.net/players/";
	private static final String COLLECTION_LOG = "https://templeosrs.com/player/collection-log.php?player=";

	private final Client client;
	private final ClientThread clientThread;
	private final ImageCapture imageCapture;

	@Inject
	ClientCommands(Client client, ClientThread clientThread, ImageCapture imageCapture)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.imageCapture = imageCapture;
	}

	void collect(List<Command> out)
	{
		// the rest need a logged in player; the client refuses to screenshot the login screen
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			out.add(new Command("client:screenshot", "Take screenshot", "", Icons.CAMERA,
				() -> imageCapture.takeScreenshot(null, "", false, true, false)));
			out.add(new Command("client:world", "Copy world", "To clipboard", Icons.CLIPBOARD,
				() -> clientThread.invoke(() -> copy("world", String.valueOf(client.getWorld())))));
			out.add(new Command("client:coordinates", "Copy coordinates", "To clipboard", Icons.CLIPBOARD, () -> clientThread.invoke(() ->
			{
				final Player player = client.getLocalPlayer();
				if (player != null)
				{
					final WorldPoint point = player.getWorldLocation();
					copy("coordinates", point.getX() + ", " + point.getY() + ", " + point.getPlane());
				}
			})));
		}
		profiles(out);
	}

	// pages about the logged in player, and their group if they are a group ironman
	private void profiles(List<Command> out)
	{
		final Player player = client.getLocalPlayer();
		if (client.getGameState() != GameState.LOGGED_IN || player == null || player.getName() == null)
		{
			return;
		}

		final String name = player.getName();
		out.add(link("client:hiscores", "Open my hiscores", "Official", HISCORES, name));
		out.add(link("client:wiseoldman", "Open my Wise Old Man profile", "", WISE_OLD_MAN, name));
		out.add(link("client:collectionlog", "Open my collection log", "TempleOSRS", COLLECTION_LOG, name));
		final ClanChannel group = client.getClanChannel(ClanID.GROUP_IRONMAN);
		if (group != null && group.getName() != null)
		{
			out.add(link("client:grouphiscores", "Open my group's hiscores", "Group ironman", GROUP_HISCORES, group.getName()));
		}
	}

	private static Command link(String key, String title, String detail, String url, String name)
	{
		// names come from the game with non-breaking spaces
		final String encoded = URLEncoder.encode(name.replace('\u00A0', ' '), StandardCharsets.UTF_8);
		return new Command(key, title, detail, Icons.LINK, () -> LinkBrowser.browse(url + encoded));
	}

	// on the client thread
	private void copy(String what, String value)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Copied " + what + ": " + value, null);
	}
}
