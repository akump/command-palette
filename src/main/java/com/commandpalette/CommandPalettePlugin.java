package com.commandpalette;

import com.google.inject.Provides;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.WorldsFetch;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Command Palette",
	description = "A hotkey-driven, searchable list of commands: jump to any side panel, look up worlds, open plugin settings",
	tags = {"command", "palette", "sidebar", "panel", "hotkey", "keyboard", "search", "navigation"}
)
public class CommandPalettePlugin extends Plugin implements KeyEventDispatcher, PalettePanel.Listener
{
	private static final Logger log = LoggerFactory.getLogger(CommandPalettePlugin.class);

	private static final int NO_KEY = KeyEvent.VK_UNDEFINED;
	static final String SIDE_PANEL_KIND = "sidepanel:";

	// The latest version with something new to tell of, and what is said about it, once, at the next login.
	// A release with nothing to announce changes the version and leaves the news empty.
	static final String NEWS_VERSION = "1.0.0";
	static final String NEWS = "";

	@Inject
	private CommandPaletteConfig config;

	@Inject
	private Hotkey hotkey;

	@Inject
	private GameTyping gameTyping;

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
	private WorldHopper worldHopper;

	@Inject
	private RecentCommands recentCommands;

	@Inject
	private PinnedCommands pinnedCommands;

	@Inject
	private CustomCommands customCommands;

	@Inject
	private Answers answers;

	@Inject
	private VariableResolver variables;

	// counts game ticks, for what is read every few
	private int ticks;

	@Inject
	private SkillIconManager skillIconManager;

	@Inject
	private Timers timers;

	@Inject
	private EventBus eventBus;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ChatMessageManager chatMessageManager;

	private PalettePanel panel;
	private SidePanel sidePanel;
	private WorldPanel worldPanel;
	// the side panel's place in the sidebar, and whether it is in the sidebar
	private NavigationButton sideButton;
	private boolean sideButtonShown;
	// A first install has no news to catch up on: it is welcomed instead, at the next login
	private boolean welcomePending;
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
		// Nothing saved at all means a first install
		if (config.lastSeenVersion().isEmpty()
			&& configManager.getConfigurationKeys(CommandPaletteConfig.GROUP + ".").isEmpty())
		{
			configManager.setConfiguration(CommandPaletteConfig.GROUP, CommandPaletteConfig.SEEN_VERSION_KEY,
				NEWS_VERSION);
			welcomePending = true;
		}
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		eventBus.register(sessionCommands);
		// a hop takes a few game ticks to carry out
		eventBus.register(worldHopper);
		// for Ground Marker Variables' answers
		eventBus.register(variables);
		createSidePanel();
		syncSidePanel();
		loadWorldLocations();
	}

	// which coast each US world is on, for its icon and description
	private void loadWorldLocations()
	{
		worldCommands.loadLocations(this::refreshWorldPanel);
	}

	private void createSidePanel()
	{
		worldPanel = new WorldPanel(() -> worldCommands.refresh(worldPanel), worldHopper::hop,
			worldCommands::setFavourite);
		final Map<SidePanel.Tab, SideTab> tabs = new EnumMap<>(SidePanel.Tab.class);
		tabs.put(SidePanel.Tab.WORLDS, worldPanel);
		tabs.put(SidePanel.Tab.TIMERS, new TimersPanel(timers::running, timers::start));
		tabs.put(SidePanel.Tab.SESSION, new SessionPanel(sessionCommands::snapshot,
			skill -> Icons.of(skillIconManager.getSkillImage(skill, true))));
		tabs.put(SidePanel.Tab.ANSWERS, new AnswersPanel(answers));
		tabs.put(SidePanel.Tab.PINNED, new PinnedPanel(this::collectCommands, this::run, pinnedCommands::load,
			pinnedCommands::toggle, recentCommands::load));
		tabs.put(SidePanel.Tab.CUSTOM, new CustomPanel(customCommands::load, customCommands::save,
			this::collectCommands));
		sidePanel = new SidePanel(tabs, hotkey::describe, () -> helpCommand.command().run());
		answers.setOnChange(() -> SwingUtilities.invokeLater(() -> sidePanel.refreshIfShowing(SidePanel.Tab.ANSWERS)));
		sideButton = NavigationButton.builder()
			.tooltip(SidePanel.NAME)
			.icon(Icons.PROMPT.image())
			.priority(8)
			.panel(sidePanel)
			.build();
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
		if (CommandPaletteConfig.GROUP.equals(event.getGroup())
			&& event.getKey().equals(CommandPaletteConfig.SIDE_PANEL_KEY))
		{
			SwingUtilities.invokeLater(this::syncSidePanel);
		}
	}

	// Another profile has settings, recently used commands and pins of its own, which no config change is sent for
	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		SwingUtilities.invokeLater(() ->
		{
			syncSidePanel();
			if (sidePanel != null)
			{
				sidePanel.refreshIfShowing(sidePanel.getSelected());
			}
		});
		loadWorldLocations();
	}

	// the side panel is in the sidebar while the setting says so
	private void syncSidePanel()
	{
		if (config.sidePanel() && !sideButtonShown)
		{
			clientToolbar.addNavigation(sideButton);
			sideButtonShown = true;
		}
		else if (!config.sidePanel() && sideButtonShown)
		{
			clientToolbar.removeNavigation(sideButton);
			sideButtonShown = false;
		}
	}

	// keep the player counts and the current world up to date while the world panel is open
	@Subscribe
	public void onWorldsFetch(WorldsFetch event)
	{
		// the list may have worlds the locations haven't been read for
		loadWorldLocations();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		refreshWorldPanel();
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			// the previews are of a player who has logged out
			customCommands.clearPreviews();
		}
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		// This state comes again after every loading screen, so each message is marked as given before it is
		if (welcomePending)
		{
			welcomePending = false;
			say(welcome(hotkey.describe()));
		}
		else if (!NEWS_VERSION.equals(config.lastSeenVersion()))
		{
			configManager.setConfiguration(CommandPaletteConfig.GROUP, CommandPaletteConfig.SEEN_VERSION_KEY,
				NEWS_VERSION);
			if (!NEWS.isEmpty())
			{
				say(NEWS);
			}
		}
	}

	// the previews of the custom commands, every few ticks
	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (ticks++ % CustomCommands.PREVIEW_TICKS == 0)
		{
			customCommands.refreshPreviews(text -> variables.resolve(text, false));
		}
	}

	private void say(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(message).build())
			.build());
	}

	/**
	 * What a first install is told: how to open the palette.
	 *
	 * @param keys the hotkey, "Ctrl+P", or null if it has no name to give
	 */
	static String welcome(String keys)
	{
		return "Command Palette: press " + (keys == null ? "its hotkey" : keys) + " anywhere in the client to open"
			+ " a side panel, a plugin's settings or a world by typing its name. Type help to see the rest.";
	}

	private void refreshWorldPanel()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (sidePanel != null)
			{
				sidePanel.refreshIfShowing(SidePanel.Tab.WORLDS);
			}
		});
	}

	@Override
	protected void shutDown()
	{
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
		eventBus.unregister(sessionCommands);
		eventBus.unregister(worldHopper);
		eventBus.unregister(variables);
		clientToolbar.removeNavigation(sideButton);
		sideButtonShown = false;
		welcomePending = false;
		sessionCommands.reset();
		worldHopper.reset();
		timers.reset();
		answers.setOnChange(() -> {});
		answers.clear();
		SwingUtilities.invokeLater(() ->
		{
			sidePanel.stop();
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
		// Without Ctrl/Cmd, a hotkey that is also a character belongs to whatever is being typed in: a text field,
		// the palette's own search included, or the game's chat or an input box of the game's
		final Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (!hotkey.hasModifier() && !e.isActionKey()
			&& (focused instanceof JTextComponent || gameTyping.isGame(focused) && gameTyping.isTyping()))
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

		final List<Command> commands = collect(frame);
		// for a custom command that takes text, before the recently used are taken out of the list
		final Map<String, Command> byKey = byKey(commands);
		if (dialog == null)
		{
			createDialog(frame);
		}
		// the pinned keep their place after the recently used, rather than moving up among them when used
		final List<Command> pinned = RecentCommands.pick(commands, pinnedCommands.load(), PinnedCommands.MAX);
		commands.removeAll(pinned);
		final List<Command> recent = config.recentFirst()
			? RecentCommands.pick(commands, recentCommands.load(), config.recentCount())
			: Collections.emptyList();
		commands.removeAll(recent);
		typedCommands.forgetVariables();
		panel.show(recent, pinned, commands, typed ->
		{
			final List<Command> typedOnes = new ArrayList<>(customCommands.forQuery(typed, byKey::get));
			typedOnes.addAll(typedCommands.forQuery(typed));
			return typedOnes;
		}, config.showIcons());
		dialog.pack();
		dialog.setLocation(location(frame));
		dialog.setVisible(true);
		panel.focusSearch();
		return true;
	}

	// for the side panel's Pinned tab
	private List<Command> collectCommands()
	{
		final JFrame frame = Sidebar.findClientFrame();
		return frame == null ? Collections.emptyList() : collect(frame);
	}

	/**
	 * Every command of the groups turned on, as things stand.
	 */
	private List<Command> collect(JFrame frame)
	{
		final List<Command> commands = new ArrayList<>();
		if (config.panelCommands())
		{
			sidebarCommands.collect(frame, commands);
		}
		if (config.sidePanel())
		{
			for (SidePanel.Tab tab : SidePanel.Tab.values())
			{
				commands.add(new Command(SIDE_PANEL_KIND + tab.name().toLowerCase(Locale.ROOT), tab.getTitle(),
					"Open in the " + SidePanel.NAME + " panel", tab.getIcon(), () -> showTab(frame, tab)));
			}
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
			worldCommands.collect(commands, world -> showWorld(frame, world));
		}
		// last, so that they can run any of the others
		customCommands.collect(commands, byKey(commands)::get);
		return commands;
	}

	/**
	 * The commands a custom one can run, by key.
	 */
	private static Map<String, Command> byKey(List<Command> commands)
	{
		final Map<String, Command> byKey = new HashMap<>();
		for (Command command : commands)
		{
			if (CustomCommands.canRun(command))
			{
				byKey.putIfAbsent(command.getKey(), command);
			}
		}
		return byKey;
	}

	// the world picked out in the side panel, for the user to click
	private void showWorld(JFrame frame, int world)
	{
		if (!config.sidePanel())
		{
			// there is no panel of this plugin's own to pick it out in
			sidebarCommands.open(frame, WorldCommands.SWITCHER_PANEL);
			return;
		}
		worldPanel.highlight(world);
		showTab(frame, SidePanel.Tab.WORLDS);
	}

	private void showTab(JFrame frame, SidePanel.Tab tab)
	{
		sidePanel.select(tab);
		sidebarCommands.open(frame, SidePanel.NAME);
	}

	private void createDialog(JFrame frame)
	{
		dialog = new JDialog(frame);
		panel = new PalettePanel(this);
		typedCommands.setOnIconLoaded(panel::repaint);
		typedCommands.setOnVariablesResolved(panel::requery);
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
