package com.commandpalette;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class CommandFilter
{
	private CommandFilter()
	{
	}

	/**
	 * The commands matching every word of the query, best matches first and otherwise in their original order.
	 * A query starting with one of {@link Query}'s symbols only finds commands of that kind.
	 */
	static List<Command> filter(List<Command> all, String typed)
	{
		final Query query = Query.parse(typed);
		final List<Command> commands = new ArrayList<>();
		for (Command command : all)
		{
			if (query.allows(command))
			{
				commands.add(command);
			}
		}

		final String needle = query.getText().toLowerCase(Locale.ROOT);
		if (needle.isEmpty())
		{
			return commands;
		}

		final String[] words = needle.split("\\s+");
		final List<Command> matches = new ArrayList<>();
		for (Command command : commands)
		{
			final String haystack = (command.getTitle() + " " + command.getDetail()).toLowerCase(Locale.ROOT);
			boolean everyWord = true;
			for (String word : words)
			{
				everyWord &= haystack.contains(word);
			}
			if (everyWord)
			{
				matches.add(command);
			}
		}
		// List.sort is stable, so equally good matches keep the order they came in
		matches.sort(Comparator.comparingInt(command -> rank(command.getTitle().toLowerCase(Locale.ROOT), needle)));
		return matches;
	}

	private static int rank(String title, String needle)
	{
		if (title.startsWith(needle))
		{
			return 0;
		}
		if (title.contains(" " + needle))
		{
			return 1;
		}
		if (title.contains(needle))
		{
			return 2;
		}
		return 3;
	}
}
