package com.commandpalette;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import net.runelite.api.MenuAction;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commands per plugin: one that turns it on or off, and one that opens its settings.
 */
class PluginCommands
{
	static final String KIND = "plugin:";
	static final String SETTINGS_KIND = "settings:";

	private static final Logger log = LoggerFactory.getLogger(PluginCommands.class);

	private final PluginManager pluginManager;
	private final ConfigManager configManager;
	private final EventBus eventBus;
	private final CommandPaletteConfig config;

	@Inject
	PluginCommands(PluginManager pluginManager, ConfigManager configManager, EventBus eventBus,
		CommandPaletteConfig config)
	{
		this.pluginManager = pluginManager;
		this.configManager = configManager;
		this.eventBus = eventBus;
		this.config = config;
	}

	/**
	 * A command per plugin that has settings, to open them in the Configuration panel.
	 */
	void collectSettings(List<Command> out)
	{
		final List<Command> commands = new ArrayList<>();
		for (Plugin plugin : pluginManager.getPlugins())
		{
			final PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
			if (descriptor == null || descriptor.hidden())
			{
				continue;
			}
			final Config pluginConfig = pluginManager.getPluginConfigProxy(plugin);
			if (pluginConfig != null && !configManager.getConfigDescriptor(pluginConfig).getItems().isEmpty())
			{
				commands.add(new Command(SETTINGS_KIND + descriptor.name(), descriptor.name(), "Open settings", Icons.COG,
					() -> openSettings(plugin)));
			}
		}
		commands.sort(Comparator.comparing(Command::getTitle, String.CASE_INSENSITIVE_ORDER));
		out.addAll(commands);
	}

	// The Configuration panel has no API, but it does open a plugin's settings when "Configure" is picked from
	// the right-click menu of one of that plugin's overlays. Tell it that happened, on an overlay nobody sees.
	private void openSettings(Plugin plugin)
	{
		final Overlay overlay = new Overlay(plugin)
		{
			@Override
			public Dimension render(Graphics2D graphics)
			{
				return null;
			}
		};
		eventBus.post(new OverlayMenuClicked(
			new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", plugin.getName()), overlay));
	}

	void collect(List<Command> out)
	{
		final List<Command> commands = new ArrayList<>();
		for (Plugin plugin : pluginManager.getPlugins())
		{
			final PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
			if (descriptor == null || descriptor.hidden() || plugin instanceof CommandPalettePlugin)
			{
				continue;
			}

			final boolean enabled = pluginManager.isPluginEnabled(plugin);
			// The configuration panel asks before turning off whatever a plugin conflicts with; leave those to it
			if (!enabled && hasEnabledConflict(plugin))
			{
				continue;
			}
			commands.add(new Command(KIND + descriptor.name(), descriptor.name(), enabled ? "Turn plugin off" : "Turn plugin on",
				enabled ? Icons.SWITCH_ON : Icons.SWITCH_OFF,
				() -> setEnabled(plugin, !enabled)));
		}
		commands.sort(Comparator.comparing(Command::getTitle, String.CASE_INSENSITIVE_ORDER));
		out.addAll(commands);
	}

	private boolean hasEnabledConflict(Plugin plugin)
	{
		for (Plugin conflict : pluginManager.conflictsForPlugin(plugin))
		{
			if (pluginManager.isPluginEnabled(conflict))
			{
				return true;
			}
		}
		return false;
	}

	private void setEnabled(Plugin plugin, boolean enabled)
	{
		if (config.confirmPluginToggle() && !confirm(plugin, enabled))
		{
			return;
		}

		pluginManager.setPluginEnabled(plugin, enabled);
		try
		{
			if (enabled)
			{
				pluginManager.startPlugin(plugin);
			}
			else
			{
				pluginManager.stopPlugin(plugin);
			}
		}
		catch (PluginInstantiationException ex)
		{
			log.warn("Error turning {} {}", plugin.getName(), enabled ? "on" : "off", ex);
		}
	}

	private static boolean confirm(Plugin plugin, boolean enabled)
	{
		final String action = enabled ? "Turn on" : "Turn off";
		final Object[] options = {action, "Cancel"};
		// Enter takes the default option, so a confirmed command is still hotkey, name, Enter, Enter
		return JOptionPane.showOptionDialog(Sidebar.findClientFrame(), action + " " + plugin.getName() + "?",
			"Command Palette", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0])
			== JOptionPane.YES_OPTION;
	}
}
