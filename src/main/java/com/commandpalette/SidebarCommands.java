package com.commandpalette;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;

/**
 * A command per side panel, plus ones to close the open panel and to hide or show the whole sidebar.
 */
class SidebarCommands
{
	static final String PANEL_KIND = "panel:";
	static final String SIDEBAR_KIND = "sidebar:";
	static final String OPEN_DETAIL = "Open panel";
	static final String SHOWING_DETAIL = "Open panel (showing)";

	// what the client sets while the sidebar is hidden, so that it starts hidden next time
	private static final String SIDEBAR_CLOSED_KEY = "clientSidebarClosed";

	private final Runnable onForcedOpen;

	@Inject
	SidebarCommands(ConfigManager configManager)
	{
		this(() -> configManager.unsetConfiguration(RuneLiteConfig.GROUP_NAME, SIDEBAR_CLOSED_KEY));
	}

	SidebarCommands(Runnable onForcedOpen)
	{
		this.onForcedOpen = onForcedOpen;
	}

	void collect(Container window, List<Command> out)
	{
		final JTabbedPane sidebar = Sidebar.find(window);
		if (sidebar == null)
		{
			return;
		}

		final boolean showing = sidebar.isVisible() && sidebar.getSelectedIndex() >= 0;
		for (int i = 0; i < sidebar.getTabCount(); i++)
		{
			final Component panel = sidebar.getComponentAt(i);
			final String tooltip = sidebar.getToolTipTextAt(i);
			final String name = tooltip == null || tooltip.isEmpty() ? "Panel " + (i + 1) : tooltip;
			final boolean current = showing && sidebar.getSelectedIndex() == i;
			out.add(new Command(PANEL_KIND + name, name, current ? SHOWING_DETAIL : OPEN_DETAIL, sidebar.getIconAt(i),
				() -> Sidebar.open(window, sidebar, panel, onForcedOpen)));
		}
		if (showing)
		{
			out.add(new Command(SIDEBAR_KIND + "close", "Close side panel", "", Icons.CLOSE, () -> sidebar.setSelectedIndex(-1)));
		}

		final JButton toggle = Sidebar.toggleButton(window, sidebar);
		if (toggle != null)
		{
			out.add(new Command(SIDEBAR_KIND + "toggle", sidebar.isVisible() ? "Hide sidebar" : "Show sidebar", "", Icons.SIDEBAR, () -> toggle.doClick(0)));
		}
	}

	/**
	 * Opens the side panel of that name, if there is one.
	 */
	void open(Container window, String name)
	{
		final JTabbedPane sidebar = Sidebar.find(window);
		for (int i = 0; sidebar != null && i < sidebar.getTabCount(); i++)
		{
			if (name.equals(sidebar.getToolTipTextAt(i)))
			{
				Sidebar.open(window, sidebar, sidebar.getComponentAt(i), onForcedOpen);
				return;
			}
		}
	}
}
