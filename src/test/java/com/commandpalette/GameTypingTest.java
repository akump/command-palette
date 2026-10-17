package com.commandpalette;

import java.awt.Canvas;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GameTypingTest
{
	private final Canvas canvas = new Canvas();
	// the chatbox: whether it is taking keys, and its input line's text; null text for no chatbox
	private boolean chatboxListening = true;
	private String chatLine;
	private boolean inputFieldFocused;

	private final GameTyping typing = new GameTyping((Client) Proxy.newProxyInstance(getClass().getClassLoader(),
		new Class<?>[]{Client.class}, (proxy, method, args) ->
		{
			switch (method.getName())
			{
				case "getCanvas":
					return canvas;
				case "getFocusedInputFieldWidget":
					return inputFieldFocused ? widget(null, null) : null;
				case "getWidget":
					if (chatLine == null)
					{
						return null;
					}
					return (int) args[0] == InterfaceID.Chatbox.UNIVERSE
						? widget(chatboxListening ? new Object[]{1} : null, null)
						: (int) args[0] == InterfaceID.Chatbox.INPUT ? widget(null, chatLine) : null;
				default:
					throw new UnsupportedOperationException(method.getName());
			}
		}));

	private static Widget widget(Object[] keyListener, String text)
	{
		return (Widget) Proxy.newProxyInstance(GameTypingTest.class.getClassLoader(), new Class<?>[]{Widget.class},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "getOnKeyListener":
						return keyListener;
					case "getText":
						return text;
					default:
						throw new UnsupportedOperationException(method.getName());
				}
			});
	}

	@Test
	public void typingGoesIntoTheChat()
	{
		chatLine = "Zezima: *";
		assertTrue(typing.isTyping());
	}

	@Test
	public void notWhileKeyRemappingKeepsTypingOutOfTheChat()
	{
		chatLine = "<col=000000>Zezima</col>: <col=0000ff>" + GameTyping.PRESS_ENTER_TO_CHAT + "</col>";
		assertFalse(typing.isTyping());
	}

	@Test
	public void anInputBoxTakesTypingWhateverTheChatIsDoing()
	{
		chatLine = GameTyping.PRESS_ENTER_TO_CHAT;
		inputFieldFocused = true;
		assertTrue(typing.isTyping());
	}

	@Test
	public void notWithoutAChatboxTakingKeys()
	{
		assertFalse(typing.isTyping());
		chatLine = "Zezima: *";
		chatboxListening = false;
		assertFalse(typing.isTyping());
	}

	@Test
	public void theWelcomeNamesTheHotkey()
	{
		assertEquals("Command Palette: press Cmd+P on the game to open a side panel, a plugin's settings"
			+ " or a world by typing its name. Type help to see the rest.", CommandPalettePlugin.welcome("Cmd+P"));
		assertTrue(CommandPalettePlugin.welcome(null).startsWith("Command Palette: press its hotkey "));
	}
}
