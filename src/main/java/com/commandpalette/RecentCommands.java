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
	static final int MAX = 8;

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
	 * The commands with the recently run ones moved to the front, latest first, and the rest as they were.
	 */
	static List<Command> recentFirst(List<Command> commands, List<String> recent)
	{
		final List<Command> rest = new ArrayList<>(commands);
		final List<Command> ordered = new ArrayList<>(commands.size());
		for (String key : recent)
		{
			for (int i = 0; i < rest.size(); i++)
			{
				if (key.equals(rest.get(i).getKey()))
				{
					ordered.add(rest.remove(i));
					break;
				}
			}
		}
		ordered.addAll(rest);
		return ordered;
	}
}
