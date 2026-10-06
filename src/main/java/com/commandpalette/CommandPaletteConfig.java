package com.commandpalette;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.util.OSType;

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

	@ConfigItem(
		keyName = "hotkey",
		name = "Open hotkey",
		description = "Opens the command palette (Cmd+P on Mac and Ctrl+P elsewhere, unless changed). Type to filter, Up/Down to move, Enter to run the selected command,"
			+ " Esc or the hotkey again to cancel.",
		position = 0
	)
	default Keybind hotkey()
	{
		return new Keybind(KeyEvent.VK_P,
			OSType.getOSType() == OSType.MacOS ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "recentFirst",
		name = "Recently used first",
		description = "List the last few commands you ran before the rest, latest first",
		position = 1
	)
	default boolean recentFirst()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showIcons",
		name = "Show icons",
		description = "Show an icon before each command's name",
		position = 2
	)
	default boolean showIcons()
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
		description = "Take a screenshot, copy your world or coordinates, sum up your session, open your hiscores and"
			+ " tracker pages, and open the screenshots, logs and RuneLite folders",
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
		description = "Work out a sum such as 35m / 4.2k or the odds of a dry streak with \"dry 1/512 600\","
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
		return true;
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
		return true;
	}

	@ConfigItem(
		keyName = "worldCommands",
		name = "World hopping",
		description = "List every world, to hop to it. PvP, high risk and Deadman worlds are left out.",
		section = commandsSection,
		position = 7
	)
	default boolean worldCommands()
	{
		return true;
	}
}
