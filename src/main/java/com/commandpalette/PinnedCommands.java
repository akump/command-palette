package com.commandpalette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

/**
 * The commands pinned to the side panel's Pinned tab, across restarts.
 */
class PinnedCommands
{
	static final int MAX = 20;

	private static final String CONFIG_KEY = "pinned";
	private static final String SEPARATOR = "\n";

	private final ConfigManager configManager;

	@Inject
	PinnedCommands(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	/**
	 * The keys of the pinned commands, in the order they were pinned.
	 */
	List<String> load()
	{
		final String saved = configManager.getConfiguration(CommandPaletteConfig.GROUP, CONFIG_KEY);
		return saved == null || saved.isEmpty() ? Collections.emptyList() : Arrays.asList(saved.split(SEPARATOR));
	}

	void toggle(Command command)
	{
		if (command.getKey() == null)
		{
			return;
		}
		final List<String> keys = toggled(load(), command.getKey());
		if (keys.isEmpty())
		{
			configManager.unsetConfiguration(CommandPaletteConfig.GROUP, CONFIG_KEY);
		}
		else
		{
			configManager.setConfiguration(CommandPaletteConfig.GROUP, CONFIG_KEY, String.join(SEPARATOR, keys));
		}
	}

	/**
	 * Unpinned if it was pinned, otherwise pinned last, so long as there is room.
	 */
	static List<String> toggled(List<String> pinned, String key)
	{
		final List<String> keys = new ArrayList<>(pinned);
		if (!keys.remove(key) && keys.size() < MAX)
		{
			keys.add(key);
		}
		return keys;
	}
}
