package com.commandpalette;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import net.runelite.client.ui.ContainableFrame;

/**
 * RuneLite has no API for listing the side panels or opening another plugin's, so this works on the client
 * window's Swing components instead: the sidebar is a tabbed pane with one tab per panel.
 */
final class Sidebar
{
	// the title bar button that hides and shows the sidebar, as ClientUI labels it in either state
	private static final String OPEN_SIDEBAR_TOOLTIP = "Open sidebar";
	private static final String CLOSE_SIDEBAR_TOOLTIP = "Close sidebar";

	private Sidebar()
	{
	}

	static JFrame findClientFrame()
	{
		for (Window window : Window.getWindows())
		{
			if (window instanceof ContainableFrame)
			{
				return (JFrame) window;
			}
		}
		return null;
	}

	/**
	 * The sidebar within the client window, or null. Panels may hold tabbed panes of their own, so this takes the
	 * outermost one with its tabs on the right.
	 */
	static JTabbedPane find(Container window)
	{
		return (JTabbedPane) search(window, c -> c instanceof JTabbedPane
			&& ((JTabbedPane) c).getTabPlacement() == JTabbedPane.RIGHT);
	}

	/**
	 * Show the panel, opening the sidebar first if it is hidden.
	 *
	 * @param onForcedOpen run if the sidebar had to be shown without the client's own button
	 */
	static void open(Container window, JTabbedPane sidebar, Component panel, Runnable onForcedOpen)
	{
		final int index = sidebar.indexOfComponent(panel);
		if (index < 0)
		{
			return;
		}
		if (sidebar.isVisible())
		{
			sidebar.setSelectedIndex(index);
			return;
		}

		final JButton toggle = toggleButton(window, sidebar);
		if (toggle != null)
		{
			// While hidden the client only notes the selection; opening the sidebar is what activates the panel
			sidebar.setSelectedIndex(index);
			toggle.doClick(0);
		}
		else
		{
			// No button without the custom title bar. The client activates a panel when the selection changes
			// on a visible sidebar, so clear it first in case this panel was already the selected one.
			sidebar.setSelectedIndex(-1);
			sidebar.setVisible(true);
			sidebar.getParent().revalidate();
			sidebar.setSelectedIndex(index);
			onForcedOpen.run();
		}
	}

	/**
	 * The client's own button for hiding or showing the sidebar, or null: it only exists with the custom title bar.
	 */
	static JButton toggleButton(Container window, JTabbedPane sidebar)
	{
		final String tooltip = sidebar.isVisible() ? CLOSE_SIDEBAR_TOOLTIP : OPEN_SIDEBAR_TOOLTIP;
		return (JButton) search(window, c -> c instanceof JButton && tooltip.equals(((JButton) c).getToolTipText()));
	}

	// breadth first, so the outermost match wins
	private static Component search(Container root, Predicate<Component> test)
	{
		final Deque<Component> queue = new ArrayDeque<>();
		queue.add(root);
		while (!queue.isEmpty())
		{
			final Component component = queue.poll();
			if (test.test(component))
			{
				return component;
			}
			if (component instanceof Container)
			{
				for (Component child : ((Container) component).getComponents())
				{
					queue.add(child);
				}
			}
		}
		return null;
	}
}
