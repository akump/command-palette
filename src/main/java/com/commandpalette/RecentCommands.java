package com.commandpalette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

/**
 * Remembers the last few commands run, across restarts.
 */
class RecentCommands
{
	// as many as the setting for how many to list goes up to, so that raising it brings older ones back
	static final int MAX = 20;

	private static final String CONFIG_KEY = "recent";
	private static final String SEPARATOR = "\n";

	private final ConfigManager configManager;

	@Inject
	RecentCommands(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	/**
	 * The keys of the commands last run, latest first.
	 */
	List<String> load()
	{
		final String saved = configManager.getConfiguration(CommandPaletteConfig.GROUP, CONFIG_KEY);
		return saved == null || saved.isEmpty() ? Collections.emptyList() : Arrays.asList(saved.split(SEPARATOR));
	}

	void record(Command command)
	{
		if (command.getKey() != null)
		{
			configManager.setConfiguration(CommandPaletteConfig.GROUP, CONFIG_KEY,
				String.join(SEPARATOR, withLatest(load(), command.getKey())));
		}
	}

	static List<String> withLatest(List<String> recent, String key)
	{
		final List<String> keys = new ArrayList<>(recent);
		keys.remove(key);
		keys.add(0, key);
		return keys.size() > MAX ? keys.subList(0, MAX) : keys;
	}

	/**
	 * The recently run ones of the commands, latest first and no more than the limit.
	 */
	static List<Command> pick(List<Command> commands, List<String> recent, int limit)
	{
		final List<Command> picked = new ArrayList<>();
		for (String key : recent)
		{
			if (picked.size() >= limit)
			{
				break;
			}
			for (Command command : commands)
			{
				if (key.equals(command.getKey()))
				{
					picked.add(command);
					break;
				}
			}
		}
		return picked;
	}
}
