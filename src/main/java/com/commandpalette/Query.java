package com.commandpalette;

/**
 * What was typed, split into an optional leading symbol that narrows the list and the words to search for.
 */
final class Query
{
	static final char PANELS = '#';
	static final char PLUGINS = '>';
	static final char WORLDS = '@';
	static final char WIKI = '?';

	private static final char NONE = 0;

	private final char prefix;
	private final String text;

	private Query(char prefix, String text)
	{
		this.prefix = prefix;
		this.text = text;
	}

	static Query parse(String typed)
	{
		final String trimmed = typed.trim();
		if (!trimmed.isEmpty())
		{
			final char first = trimmed.charAt(0);
			if (first == PANELS || first == PLUGINS || first == WORLDS || first == WIKI)
			{
				return new Query(first, trimmed.substring(1).trim());
			}
		}
		return new Query(NONE, trimmed);
	}

	boolean hasPrefix()
	{
		return prefix != NONE;
	}

	boolean isWiki()
	{
		return prefix == WIKI;
	}

	String getText()
	{
		return text;
	}

	/**
	 * Whether the prefix, if there is one, lets this command through.
	 */
	boolean allows(Command command)
	{
		switch (prefix)
		{
			case NONE:
				return true;
			case PANELS:
				return hasKind(command, SidebarCommands.PANEL_KIND) || hasKind(command, SidebarCommands.SIDEBAR_KIND);
			case PLUGINS:
				return hasKind(command, PluginCommands.KIND) || hasKind(command, PluginCommands.SETTINGS_KIND);
			case WORLDS:
				return hasKind(command, WorldCommands.KIND);
			default:
				return false;
		}
	}

	private static boolean hasKind(Command command, String kind)
	{
		return command.getKey() != null && command.getKey().startsWith(kind);
	}
}
