package com.commandpalette;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.JOptionPane;

/**
 * The command that explains the rest.
 */
class HelpCommand
{
	private static final int WIDTH = 560;

	private final CommandPaletteConfig config;

	@Inject
	HelpCommand(CommandPaletteConfig config)
	{
		this.config = config;
	}

	Command command()
	{
		return new Command("help", "Help", "What to type", Icons.HELP,
			() -> JOptionPane.showMessageDialog(Sidebar.findClientFrame(), text(), "Command Palette",
				JOptionPane.PLAIN_MESSAGE));
	}

	String text()
	{
		// as wide as the commands need, so that a long tip wraps rather than stretching the window
		final StringBuilder html = new StringBuilder("<html><table cellpadding=2 width=" + WIDTH + ">");
		for (Section section : sections())
		{
			html.append("<tr><td colspan=2><b>").append(section.getTitle()).append("</b></td></tr>");
			for (Entry entry : section.getEntries())
			{
				if (entry.getKeys() == null)
				{
					html.append("<tr><td colspan=2>").append(entry.getText()).append("</td></tr>");
				}
				else
				{
					html.append("<tr><td>").append(entry.getKeys()).append("&nbsp;&nbsp;</td><td>")
						.append(entry.getText()).append("</td></tr>");
				}
			}
		}
		return html.append("</table></html>").toString();
	}

	/**
	 * The commands of the groups that are turned on, and how to get more out of them. The text is HTML.
	 */
	List<Section> sections()
	{
		final Section commands = new Section("Commands");
		if (config.panelCommands())
		{
			commands.row("<i>panel name</i>", "Open that side panel");
			commands.row("close, hide, show", "Close the open panel, or hide or show the sidebar");
		}
		if (config.clientCommands())
		{
			commands.row("screenshot", "Take a screenshot (logged in)");
			commands.row("copy", "Copy your world or coordinates (logged in)");
			commands.row("session", "Time logged in and xp gained (logged in)");
			commands.row("open my", "Your hiscores and tracker pages (logged in)");
		}
		if (config.pluginCommands())
		{
			commands.row("<i>plugin name</i>", "Turn that plugin on or off");
		}
		if (config.pluginSettingsCommands())
		{
			commands.row("<i>plugin name</i> settings", "Open that plugin's settings");
		}
		if (config.worldCommands())
		{
			commands.row("world 302, wintertodt", config.sidePanel()
				? "Pick out a world in the side panel, to click" : "Look up a world; Enter opens the World Switcher");
			if (config.sidePanel())
			{
				commands.row("previous world", "Pick out the world you were on before");
			}
		}
		if (config.sidePanel())
		{
			commands.row("timers, session, answers, pinned, custom", "Open that tab of the Command Palette side panel");
		}
		if (config.lookupCommands())
		{
			commands.row("35m / 4.2k", "Work out a sum; = first for a lone number");
			commands.row("dry 1/512 600", "The odds of going that dry");
			commands.row("tax 12m", "The Grand Exchange's tax on selling at that price");
			commands.row("ticks 250", "Game ticks as time; ticks 2m30s for the reverse");
			commands.row("alch rune platebody", "An item's price and alch value (logged in)");
			commands.row("xp 92 slayer", "The xp left to that level (logged in)");
			commands.row("{lvl_agility}", "A variable's value: {rsn}, {world}, {xp_slayer}, and with Ground Marker"
				+ " Variables {kc zulrah} and the rest");
		}
		if (config.timerCommands())
		{
			commands.row("timer 5m herb run", "Notify you in 5 minutes; cancel timer to stop it");
		}
		commands.row("help", "Show this list in a window");
		if (config.wikiSearch())
		{
			commands.row("<i>anything else</i>", "Search the wiki for it");
		}

		final Section narrowing = new Section("Narrowing the list");
		narrowing.row("# loot", "Only side panels");
		narrowing.row("&gt; loot", "Only plugins");
		narrowing.row("@ 302", "Only worlds");
		narrowing.row("? abyssal whip", "Only the wiki search");

		final Section tips = new Section("Tips");
		tips.note("The words on the right of each row are searchable: try <b>panel</b>,"
			+ " <b>plugin</b> or <b>favourite</b>.");
		if (config.recentFirst())
		{
			tips.note("The commands you ran last are listed first, then the ones you pinned, each above a line.");
		}
		if (config.sidePanel())
		{
			tips.note("Sums, taxes and prices you work out are kept in the side panel's Answers tab, and the"
				+ " commands you use most can be pinned in its Pinned tab.");
			tips.note("Make commands of your own in its Custom tab: a site to search, a timer you start often,"
				+ " a shortcut to another command or text to copy.");
		}
		tips.note("Change the hotkey and turn groups of commands off in the plugin's settings.");
		return List.of(commands, narrowing, tips);
	}

	static final class Section
	{
		private final String title;
		private final List<Entry> entries = new ArrayList<>();

		private Section(String title)
		{
			this.title = title;
		}

		private void row(String keys, String text)
		{
			entries.add(new Entry(keys, text));
		}

		private void note(String text)
		{
			entries.add(new Entry(null, text));
		}

		String getTitle()
		{
			return title;
		}

		List<Entry> getEntries()
		{
			return entries;
		}
	}

	/**
	 * What to type and what it does, or with no keys a tip.
	 */
	static final class Entry
	{
		private final String keys;
		private final String text;

		private Entry(String keys, String text)
		{
			this.keys = keys;
			this.text = text;
		}

		String getKeys()
		{
			return keys;
		}

		String getText()
		{
			return text;
		}
	}
}
