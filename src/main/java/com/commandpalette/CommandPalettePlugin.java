package com.commandpalette;

import com.google.inject.Provides;
import java.awt.Container;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Command Palette",
	description = "A hotkey-driven, searchable list of commands: jump to any side panel, hop worlds, open plugin settings",
	tags = {"command", "palette", "sidebar", "panel", "hotkey", "keyboard", "search", "navigation"}
)
public class CommandPalettePlugin extends Plugin implements KeyEventDispatcher, PalettePanel.Listener
{
	private static final Logger log = LoggerFactory.getLogger(CommandPalettePlugin.class);

	private static final int NO_KEY = KeyEvent.VK_UNDEFINED;

	@Inject
	private CommandPaletteConfig config;

	@Inject
	private Hotkey hotkey;

	@Inject
	private SidebarCommands sidebarCommands;

	@Inject
	private ClientCommands clientCommands;

	@Inject
	private SessionCommands sessionCommands;

	@Inject
	private TypedCommands typedCommands;

	@Inject
	private HelpCommand helpCommand;

	@Inject
	private PluginCommands pluginCommands;

	@Inject
	private WorldCommands worldCommands;

	@Inject
	private RecentCommands recentCommands;

	@Inject
	private Timers timers;

	@Inject
	private EventBus eventBus;

	@Inject
	private ConfigManager configManager;

	private PalettePanel panel;
	private JDialog dialog;
	// the hotkey's key while it is still held down after opening or closing the palette
	private int heldKey = NO_KEY;

	@Provides
	CommandPaletteConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CommandPaletteConfig.class);
	}

	@Override
	protected void startUp()
	{
		// Not the KeyManager: that only hears keys pressed on the game, and the palette should also open from a
		// side panel, and close from its own search field
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		// a hop takes a few game ticks to carry out
		eventBus.register(worldCommands);
		eventBus.register(sessionCommands);
	}

	// The help in the settings is in text boxes, which can be typed in: put back whatever is changed
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CommandPaletteConfig.GROUP.equals(event.getGroup()) && event.getKey().startsWith("help")
			&& event.getNewValue() != null)
		{
			configManager.unsetConfiguration(CommandPaletteConfig.GROUP, event.getKey());
		}
	}

	@Override
	protected void shutDown()
	{
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
		eventBus.unregister(worldCommands);
		eventBus.unregister(sessionCommands);
		worldCommands.reset();
		sessionCommands.reset();
		timers.reset();
		SwingUtilities.invokeLater(() ->
		{
			if (dialog != null)
			{
				dialog.dispose();
				dialog = null;
				panel = null;
			}
			heldKey = NO_KEY;
		});
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent e)
	{
		if (e.getID() == KeyEvent.KEY_PRESSED)
		{
			if (!hotkey.matches(e))
			{
				heldKey = NO_KEY;
				return false;
			}
			// Key repeat while the hotkey is held shouldn't flick the palette open and shut
			if (heldKey != e.getKeyCode() && !toggle(e))
			{
				return false;
			}
			heldKey = e.getKeyCode();
			return true;
		}

		// Swallow the rest of the hotkey's own keystroke so it isn't typed into the search or the game
		if (heldKey != NO_KEY)
		{
			if (e.getID() == KeyEvent.KEY_RELEASED && e.getKeyCode() == heldKey)
			{
				heldKey = NO_KEY;
				return true;
			}
			return e.getID() == KeyEvent.KEY_TYPED;
		}
		return false;
	}

	private boolean toggle(KeyEvent e)
	{
		// Without Ctrl/Cmd, a hotkey that is also a character belongs to whatever text field is being typed in,
		// the palette's own search included
		if (!hotkey.hasModifier() && !e.isActionKey()
			&& KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent)
		{
			return false;
		}
		if (isOpen())
		{
			cancel();
			return true;
		}
		return open();
	}

	private boolean isOpen()
	{
		return dialog != null && dialog.isVisible();
	}

	private boolean open()
	{
		final JFrame frame = Sidebar.findClientFrame();
		if (frame == null)
		{
			return false;
		}

		final List<Command> commands = new ArrayList<>();
		if (config.panelCommands())
		{
			sidebarCommands.collect(frame, commands);
		}
		if (config.clientCommands())
		{
			clientCommands.collect(commands);
			sessionCommands.collect(commands);
		}
		timers.collect(commands);
		commands.add(helpCommand.command());
		if (config.pluginCommands())
		{
			pluginCommands.collect(commands);
		}
		if (config.pluginSettingsCommands())
		{
			pluginCommands.collectSettings(commands);
		}
		if (config.worldCommands())
		{
			worldCommands.collect(commands);
		}

		if (dialog == null)
		{
			createDialog(frame);
		}
		final List<Command> recent = config.recentFirst()
			? RecentCommands.pick(commands, recentCommands.load(), config.recentCount())
			: Collections.emptyList();
		commands.removeAll(recent);
		panel.show(recent, commands, typedCommands::forQuery, config.showIcons());
		dialog.pack();
		dialog.setLocation(location(frame));
		dialog.setVisible(true);
		panel.focusSearch();
		return true;
	}

	private void createDialog(JFrame frame)
	{
		dialog = new JDialog(frame);
		panel = new PalettePanel(this);
		typedCommands.setOnIconLoaded(panel::repaint);
		dialog.setUndecorated(true);
		dialog.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
		dialog.setResizable(false);
		dialog.setContentPane(panel);
		dialog.addWindowFocusListener(new WindowAdapter()
		{
			@Override
			public void windowLostFocus(WindowEvent e)
			{
				cancel();
			}
		});
	}

	// near the top of the game, clear of the sidebar
	private Point location(JFrame frame)
	{
		final Container content = frame.getContentPane();
		final JTabbedPane sidebar = Sidebar.find(frame);
		final int sidebarWidth = sidebar != null && sidebar.isVisible() ? sidebar.getWidth() : 0;
		final Point origin = content.getLocationOnScreen();
		return new Point(
			origin.x + Math.max(0, (content.getWidth() - sidebarWidth - dialog.getWidth()) / 2),
			origin.y + content.getHeight() / 6);
	}

	@Override
	public void run(Command command)
	{
		// close first, so that the command acts on the client window and not on the palette
		cancel();
		recentCommands.record(command);
		try
		{
			command.run();
		}
		catch (RuntimeException ex)
		{
			log.warn("Command \"{}\" failed", command.getTitle(), ex);
		}
	}

	@Override
	public void cancel()
	{
		if (isOpen())
		{
			dialog.setVisible(false);
		}
	}

	@Override
	public void resized()
	{
		if (dialog != null)
		{
			dialog.pack();
		}
	}
}
