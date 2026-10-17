package com.commandpalette;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * A command the user has made from one of the palette's own actions, saved as json.
 */
final class CustomCommand
{
	/**
	 * What a custom command does. Each is something the palette can already do: none of them sends anything to the
	 * game.
	 */
	enum Action
	{
		LINK("Open a link"),
		TIMER("Start a timer"),
		COMMAND("Run a palette command"),
		COPY("Copy text");

		private final String label;

		Action(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	// in a link, where what is typed after the command's name goes
	static final String TEXT = "{text}";
	static final int MAX_NAME = 40;

	private final String name;
	private final Action action;
	private final String value;

	CustomCommand(String name, Action action, String value)
	{
		this.name = name;
		this.action = action;
		this.value = value;
	}

	String getName()
	{
		return name;
	}

	Action getAction()
	{
		return action;
	}

	/**
	 * The link, the timer ("80m herb run"), the key of the palette command, or the text to copy.
	 */
	String getValue()
	{
		return value;
	}

	String getKey()
	{
		return CustomCommands.KIND + name.toLowerCase(Locale.ROOT);
	}

	// commands are read afresh from the settings each time, so they are told apart by what they are
	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof CustomCommand))
		{
			return false;
		}
		final CustomCommand that = (CustomCommand) other;
		return name.equals(that.name) && action == that.action && value.equals(that.value);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, action, value);
	}

	/**
	 * Whether what is typed after its name is put into its link.
	 */
	boolean takesText()
	{
		return action == Action.LINK && value.contains(TEXT);
	}

	/**
	 * What it does, as the palette shows it beside the name.
	 *
	 * @param titleOf the title of the palette command with that key, or null if there is none now
	 */
	String describe(Function<String, String> titleOf)
	{
		switch (action)
		{
			case LINK:
				return takesText() ? "Search " + host(value) : "Open " + host(value);
			case TIMER:
				final Timers.Request timer = Timers.parse(value);
				return timer == null ? "Timer" : "Start " + timer.describe();
			case COMMAND:
				final String title = titleOf.apply(value);
				return title == null ? "Command not available now" : "Run " + title;
			default:
				return "Copy " + value;
		}
	}

	// "prices.runescape.wiki", for describing a link
	static String host(String url)
	{
		String host = url.replaceFirst("^[a-zA-Z]+://", "");
		final int end = host.indexOf('/');
		host = end < 0 ? host : host.substring(0, end);
		return host.startsWith("www.") ? host.substring(4) : host;
	}

	/**
	 * What is wrong with it, to be put right before it is saved, or null if nothing is.
	 *
	 * @param others  the names of the other custom commands
	 * @param exists  whether there is a palette command with that key
	 */
	static String problem(String name, Action action, String value, Collection<String> others,
		Function<String, Boolean> exists)
	{
		final String trimmed = name.trim();
		if (trimmed.isEmpty())
		{
			return "Give it a name to type in the palette.";
		}
		if (trimmed.length() > MAX_NAME)
		{
			return "Keep the name to " + MAX_NAME + " characters.";
		}
		for (String other : others)
		{
			if (other.equalsIgnoreCase(trimmed))
			{
				return "There is already a command called " + other + ".";
			}
		}
		final String v = value.trim();
		switch (action)
		{
			case LINK:
				return v.matches("(?i)https?://[^\\s/]+.*") ? null
					: "A link starts with https:// and has no spaces; put " + TEXT + " where the search goes.";
			case TIMER:
				return Timers.parse(v) != null ? null
					: "Start with a length of time, up to a day, then what it is for: 80m herb run.";
			case COMMAND:
				return exists.apply(v) ? null : "Pick a command to run.";
			default:
				return v.isEmpty() ? "Type the text to copy." : null;
		}
	}
}
