package com.commandpalette;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import net.runelite.client.config.Keybind;

/**
 * The command that explains the rest.
 */
class HelpCommand
{
	private final CommandPaletteConfig config;

	@Inject
	HelpCommand(CommandPaletteConfig config)
	{
		this.config = config;
	}

	Command command()
	{
		return new Command("help", "Help", "Keys and commands", Icons.HELP,
			() -> JOptionPane.showMessageDialog(Sidebar.findClientFrame(), text(), "Command Palette",
				JOptionPane.PLAIN_MESSAGE));
	}

	String text()
	{
		final StringBuilder html = new StringBuilder("<html><table cellpadding=2>");
		heading(html, "Keys");
		row(html, describe(config.hotkey()), "Open the palette, or close it");
		row(html, "Type", "Filter the commands; every word must match");
		row(html, "Up / Down, Tab", "Move the selection");
		row(html, "Page Up / Down", "Move a page at a time");
		row(html, "Enter", "Run the selected command");
		row(html, "Esc", "Cancel");

		heading(html, "Commands");
		if (config.panelCommands())
		{
			row(html, "<i>panel name</i>", "Open that side panel");
			row(html, "close, hide, show", "Close the open panel, or hide or show the sidebar");
		}
		if (config.clientCommands())
		{
			row(html, "screenshot", "Take a screenshot (logged in)");
			row(html, "copy", "Copy your world or coordinates (logged in)");
			row(html, "session", "Time logged in and xp gained (logged in)");
			row(html, "open my", "Your hiscores and tracker pages (logged in)");
			row(html, "folder", "Open the screenshots, logs or RuneLite folder");
		}
		if (config.pluginCommands())
		{
			row(html, "<i>plugin name</i>", "Turn that plugin on or off");
		}
		if (config.pluginSettingsCommands())
		{
			row(html, "<i>plugin name</i> settings", "Open that plugin's settings");
		}
		if (config.worldCommands())
		{
			row(html, "world 302", "Hop to that world");
		}
		if (config.lookupCommands())
		{
			row(html, "35m / 4.2k", "Work out a sum; = first for a lone number");
			row(html, "dry 1/512 600", "The odds of going that dry");
			row(html, "alch rune platebody", "An item's price and alch value (logged in)");
			row(html, "xp 92 slayer", "The xp left to that level (logged in)");
		}
		if (config.wikiSearch())
		{
			row(html, "<i>anything else</i>", "Search the wiki for it");
		}

		heading(html, "Narrowing the list");
		row(html, "# loot", "Only side panels");
		row(html, "&gt; loot", "Only plugins");
		row(html, "@ 302", "Only worlds");
		row(html, "? abyssal whip", "Only the wiki search");

		heading(html, "Tips");
		html.append("<tr><td colspan=2>The words on the right of each row are searchable: try <b>panel</b>,"
			+ " <b>plugin</b> or <b>hop</b>.</td></tr>");
		if (config.recentFirst())
		{
			html.append("<tr><td colspan=2>The commands you ran last are listed first.</td></tr>");
		}
		html.append("<tr><td colspan=2>Change the hotkey and turn groups of commands off in the plugin's settings."
			+ "</td></tr>");
		return html.append("</table></html>").toString();
	}

	// Spelled out: on a Mac the keybind's own text uses symbols that the client's font doesn't have
	static String describe(Keybind keybind)
	{
		final int modifiers = keybind.getModifiers();
		final StringBuilder text = new StringBuilder();
		if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0)
		{
			text.append("Ctrl+");
		}
		if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0)
		{
			text.append("Alt+");
		}
		if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0)
		{
			text.append("Shift+");
		}
		if ((modifiers & InputEvent.META_DOWN_MASK) != 0)
		{
			text.append("Cmd+");
		}
		if (keybind.getKeyCode() == KeyEvent.VK_UNDEFINED)
		{
			return text.length() == 0 ? "Not set" : text.substring(0, text.length() - 1);
		}
		return text.append(KeyEvent.getKeyText(keybind.getKeyCode())).toString();
	}

	private static void heading(StringBuilder html, String text)
	{
		html.append("<tr><td colspan=2><b>").append(text).append("</b></td></tr>");
	}

	private static void row(StringBuilder html, String keys, String action)
	{
		html.append("<tr><td>").append(keys).append("&nbsp;&nbsp;</td><td>").append(action).append("</td></tr>");
	}
}
