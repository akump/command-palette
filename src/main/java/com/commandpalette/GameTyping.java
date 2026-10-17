package com.commandpalette;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/**
 * Whether the game would take a typed character as text, so that a hotkey without Ctrl/Cmd leaves it be.
 */
class GameTyping
{
	// what the core Key Remapping plugin shows in the chatbox while it keeps typing out of the chat
	static final String PRESS_ENTER_TO_CHAT = "Press Enter to Chat...";

	private final Client client;

	@Inject
	GameTyping(Client client)
	{
		this.client = client;
	}

	/**
	 * Whether a key typed on the game would type into it: into an input box, such as the bank's search or an
	 * "Enter amount", or into the chat, unless Key Remapping is keeping typing out of the chat until Enter is
	 * pressed.
	 */
	boolean isTyping()
	{
		if (client.getFocusedInputFieldWidget() != null)
		{
			return true;
		}
		final Widget chatbox = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		if (chatbox == null || chatbox.getOnKeyListener() == null)
		{
			return false;
		}
		final Widget line = client.getWidget(InterfaceID.Chatbox.INPUT);
		return line == null || !Text.removeTags(line.getText()).endsWith(PRESS_ENTER_TO_CHAT);
	}
}
