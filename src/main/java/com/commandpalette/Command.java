package com.commandpalette;

import javax.swing.Icon;

/**
 * One row of the palette: something that can be found by name and run.
 */
final class Command
{
	private final String key;
	private final String title;
	private final String detail;
	private final Icon icon;
	private final Runnable action;

	/**
	 * @param key    identifies the command from one listing to the next, so that it can be remembered as recently
	 *               used; null for a command that shouldn't be
	 * @param title  what the command is called, e.g. the panel's name
	 * @param detail what running it does, shown dimmed beside the title and searchable too
	 * @param icon   a 16x16 icon, or null
	 * @param action run on the Swing thread once the palette has closed
	 */
	Command(String key, String title, String detail, Icon icon, Runnable action)
	{
		this.key = key;
		this.title = title;
		this.detail = detail;
		this.icon = icon;
		this.action = action;
	}

	/**
	 * A command that isn't remembered as recently used.
	 */
	Command(String title, String detail, Icon icon, Runnable action)
	{
		this(null, title, detail, icon, action);
	}

	String getKey()
	{
		return key;
	}

	String getTitle()
	{
		return title;
	}

	String getDetail()
	{
		return detail;
	}

	Icon getIcon()
	{
		return icon;
	}

	void run()
	{
		action.run();
	}

	@Override
	public String toString()
	{
		return title;
	}
}
