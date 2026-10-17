package com.commandpalette;

import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(CommandPaletteConfig.GROUP)
public interface CommandPaletteConfig extends Config
{
	String GROUP = "commandpalette";

	@ConfigSection(
		name = "Commands",
		description = "Which commands the palette lists",
		position = 10
	)
	String commandsSection = "commands";

	@ConfigSection(
		name = "Help",
		description = "How to find out what the palette can do",
		position = 20
	)
	String helpSection = "help";

	@ConfigItem(
		keyName = "hotkey",
		name = "Open hotkey",
		description = "Opens the command palette. Only the key is used; the modifier comes from the next setting."
			+ " Type to filter, Up/Down to move, Enter to run the selected command, Esc or the hotkey again to cancel.",
		position = 0
	)
	default Keybind hotkey()
	{
		return new Keybind(KeyEvent.VK_P, 0);
	}

	@ConfigItem(
		keyName = "requireModifier",
		name = "Require Ctrl/Cmd",
		description = "Require the platform modifier (Cmd on Mac, Ctrl on Windows) together with the open hotkey."
			+ " Follows the Platform Keys plugin's key profile when that plugin is on.",
		position = 1
	)
	default boolean requireModifier()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recentFirst",
		name = "Recently used first",
		description = "List the last few commands you ran in a section of their own before the rest, latest first",
		position = 2
	)
	default boolean recentFirst()
	{
		return true;
	}

	@Range(
		min = 1,
		max = RecentCommands.MAX
	)
	@ConfigItem(
		keyName = "recentCount",
		name = "Recently used count",
		description = "How many recently used commands to list first",
		position = 3
	)
	default int recentCount()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "showIcons",
		name = "Show icons",
		description = "Show an icon before each command's name, and a clock after a recently used one",
		position = 4
	)
	default boolean showIcons()
	{
		return true;
	}

	String SIDE_PANEL_KEY = "sidePanel";

	@ConfigItem(
		keyName = SIDE_PANEL_KEY,
		name = "Side panel",
		description = "Add a Command Palette panel to the sidebar, with tabs for the worlds (to hop with a click),"
			+ " running timers, this session's xp, the palette's recent answers, your pinned commands and"
			+ " commands of your own",
		position = 5
	)
	default boolean sidePanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "panelCommands",
		name = "Side panels",
		description = "List every side panel, to open it, and commands to close the open one and to hide or show the sidebar",
		section = commandsSection,
		position = 0
	)
	default boolean panelCommands()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clientCommands",
		name = "Client utilities",
		description = "Take a screenshot, copy your world or coordinates, sum up your session, and open your"
			+ " hiscores and tracker pages",
		section = commandsSection,
		position = 1
	)
	default boolean clientCommands()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lookupCommands",
		name = "Lookups and sums",
		description = "Work out a sum such as 35m / 4.2k, the odds of a dry streak with \"dry 1/512 600\","
			+ " the Grand Exchange's tax with \"tax 12m\" or game ticks as time with \"ticks 250\","
			+ " and when logged in look up an item with \"alch rune platebody\""
			+ " or the xp left to a level with \"xp 92 slayer\"",
		section = commandsSection,
		position = 2
	)
	default boolean lookupCommands()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wikiSearch",
		name = "Wiki search",
		description = "Offer to search the wiki for whatever is typed, after the commands matching it."
			+ " Starting with ? searches the wiki whether or not this is on.",
		section = commandsSection,
		position = 6
	)
	default boolean wikiSearch()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pluginCommands",
		name = "Plugins on/off",
		description = "List every plugin, to turn it on or off",
		section = commandsSection,
		position = 3
	)
	default boolean pluginCommands()
	{
		return false;
	}

	@ConfigItem(
		keyName = "confirmPluginToggle",
		name = "Confirm plugins on/off",
		description = "Ask before turning a plugin on or off from the palette",
		section = commandsSection,
		position = 4
	)
	default boolean confirmPluginToggle()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pluginSettingsCommands",
		name = "Plugin settings",
		description = "List every plugin that has settings, to open them in the Configuration panel",
		section = commandsSection,
		position = 5
	)
	default boolean pluginSettingsCommands()
	{
		return false;
	}

	@ConfigItem(
		keyName = "worldCommands",
		name = "Worlds",
		description = "List every world, to look one up by its number or activity, the World Hopper plugin's"
			+ " favourites first. Running one highlights it in the side panel's Worlds tab, where a click hops,"
			+ " or with the side panel off opens the World Hopper plugin's panel."
			+ " PvP, high risk and Deadman worlds are left out.",
		section = commandsSection,
		position = 7
	)
	default boolean worldCommands()
	{
		return true;
	}

	@ConfigItem(
		keyName = "timerCommands",
		name = "Timers",
		description = "Start a timer with \"timer 5m herb run\", which sends a notification when it runs out."
			+ " A timer that is running can be cancelled whether or not this is on.",
		section = commandsSection,
		position = 8
	)
	default boolean timerCommands()
	{
		return true;
	}

	@ConfigItem(
		keyName = "help",
		name = "Help command",
		description = "Where to find what can be typed into the palette",
		section = helpSection,
		position = 0
	)
	default String help()
	{
		return "Open the palette, type help and press Enter. The Help command lists every command.";
	}

	String SEEN_VERSION_KEY = "lastSeenVersion";

	@ConfigItem(
		keyName = SEEN_VERSION_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String lastSeenVersion()
	{
		return "";
	}
}
