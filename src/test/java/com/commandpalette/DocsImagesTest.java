package com.commandpalette;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.laf.RuneLiteLAF;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldType;
import org.junit.Assume;
import org.junit.Test;

/**
 * Renders the README's still pictures to build/docs: the palette, and the side panel's five tabs side by side.
 * Copy them into docs/ after changing how either looks. The content is stand-ins for what a running client would
 * show, worked out by the plugin's own code where it can be.
 */
public class DocsImagesTest
{
	// drawn at twice the size so that the README's copies stay sharp on high density screens
	private static final int SCALE = 2;
	private static final int GAP = 12;
	private static final int SIDE_PANEL_HEIGHT = 560;
	private static final String PLUGINS = "/net/runelite/client/plugins/";

	private static Icon icon(String resource)
	{
		try
		{
			return Icons.of(ImageIO.read(DocsImagesTest.class.getResourceAsStream(resource)));
		}
		catch (Exception e)
		{
			throw new IllegalStateException(resource, e);
		}
	}

	private static Icon skill(Skill skill)
	{
		return icon("/skill_icons_small/" + skill.getName().toLowerCase(Locale.ROOT) + ".png");
	}

	private static World world(int id, String activity, WorldRegion region, int players, boolean members)
	{
		return World.builder().id(id).activity(activity).region(region).players(players)
			.types(members ? EnumSet.of(WorldType.MEMBERS) : EnumSet.noneOf(WorldType.class)).build();
	}

	@Test
	public void renderDocs() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		RuneLiteLAF.setup();
		final File dir = new File("build/docs");
		dir.mkdirs();
		final BufferedImage[] images = new BufferedImage[3];
		SwingUtilities.invokeAndWait(() ->
		{
			images[0] = palette();
			images[1] = sidePanel();
			images[2] = customCommands();
		});
		ImageIO.write(images[0], "png", new File(dir, "palette.png"));
		ImageIO.write(images[1], "png", new File(dir, "side-panel.png"));
		ImageIO.write(images[2], "png", new File(dir, "custom-commands.png"));
	}

	// the palette as it opens, the recently used commands first, then the pinned
	private static BufferedImage palette()
	{
		final List<Command> recent = List.of(
			new Command("World 330", "Favourite - Members - US West - House Party",
				WorldIcons.of(WorldLocation.US_WEST, true), () -> {}),
			new Command("Loot Tracker", SidebarCommands.OPEN_DETAIL, icon(PLUGINS + "loottracker/panel_icon.png"),
				() -> {}),
			new Command("Timers", "Open in the Command Palette panel", Icons.TIMER, () -> {}));
		final List<Command> pinned = List.of(
			new Command("Screen Markers", "Open settings", Icons.COG, () -> {}),
			new Command("Previous world", "World 302", Icons.RECENT, () -> {}));
		final List<Command> commands = new ArrayList<>();
		final String[][] panels = {
			{"Configuration", "config/config_icon.png"},
			{"Grand Exchange", "grandexchange/ge_icon.png"},
			{"World Switcher", "worldhopper/icon.png"},
			{"Screen Markers", "screenmarkers/panel_icon.png"},
			{"Notes", "notes/notes_icon.png"},
		};
		for (String[] panel : panels)
		{
			commands.add(new Command(panel[0], SidebarCommands.OPEN_DETAIL, icon(PLUGINS + panel[1]), () -> {}));
		}
		commands.add(new Command("Take screenshot", "", Icons.CAMERA, () -> {}));
		commands.add(new Command("Copy world", "To clipboard", Icons.CLIPBOARD, () -> {}));
		commands.add(new Command("Session summary", "Show in chat", Icons.CLOCK, () -> {}));
		commands.add(new Command("Help", "What to type", Icons.HELP, () -> {}));

		final PalettePanel panel = new PalettePanel(new PalettePanel.Listener()
		{
			@Override
			public void run(Command command)
			{
			}

			@Override
			public void cancel()
			{
			}

			@Override
			public void resized()
			{
			}
		});
		panel.show(recent, pinned, commands, typed -> List.of(), true);
		return paint(panel, null);
	}

	private static BufferedImage sidePanel()
	{
		final List<BufferedImage> tabs = new ArrayList<>();
		for (SidePanel.Tab tab : SidePanel.Tab.values())
		{
			tabs.add(sidePanel(tab, null));
		}
		return sideBySide(tabs);
	}

	// the Custom tab's list of commands, and its editor making one
	private static BufferedImage customCommands()
	{
		return sideBySide(List.of(sidePanel(SidePanel.Tab.CUSTOM, null), sidePanel(SidePanel.Tab.CUSTOM, panel ->
		{
			panel.openEditor(null);
			panel.getNameField().setText("ge");
			panel.getValueField().setText("https://prices.runescape.wiki/osrs/all-items?search={text}");
		}), emptyCustom()));
	}

	// the Custom tab before any are made, offering examples
	private static BufferedImage emptyCustom()
	{
		final Map<SidePanel.Tab, SideTab> contents = new EnumMap<>(SidePanel.Tab.class);
		for (SidePanel.Tab tab : SidePanel.Tab.values())
		{
			contents.put(tab, tab == SidePanel.Tab.CUSTOM ? new CustomPanel(List::of, commands -> {}, List::of)
				: new AnswersPanel(new Answers()));
		}
		final SidePanel side = new SidePanel(contents, () -> "Cmd+P", () -> {});
		side.select(SidePanel.Tab.CUSTOM);
		final BufferedImage image = paint(side, SIDE_PANEL_HEIGHT);
		side.stop();
		return image;
	}

	private static BufferedImage sideBySide(List<BufferedImage> tabs)
	{
		final int width = tabs.get(0).getWidth();
		final BufferedImage image = new BufferedImage(tabs.size() * (width + GAP * SCALE) + GAP * SCALE,
			tabs.get(0).getHeight() + 2 * GAP * SCALE, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		g.setColor(ColorScheme.DARKER_GRAY_COLOR);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		for (int i = 0; i < tabs.size(); i++)
		{
			g.drawImage(tabs.get(i), GAP * SCALE + i * (width + GAP * SCALE), GAP * SCALE, null);
		}
		g.dispose();
		return image;
	}

	/**
	 * @param editing does something with the Custom tab before it is drawn, or null
	 */
	private static BufferedImage sidePanel(SidePanel.Tab shown, Consumer<CustomPanel> editing)
	{
		final Map<SidePanel.Tab, SideTab> contents = new EnumMap<>(SidePanel.Tab.class);
		contents.put(SidePanel.Tab.WORLDS, worlds());
		final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		final Timers timers = new Timers(executor, null, null, null);
		timers.start(Timers.parse("5m herb run"));
		timers.start(Timers.parse("50m birdhouses"));
		timers.start(Timers.parse("1h 20m farming contract"));
		contents.put(SidePanel.Tab.TIMERS, new TimersPanel(timers::running, timers::start));
		contents.put(SidePanel.Tab.SESSION, new SessionPanel(then -> then.accept(session()), DocsImagesTest::skill));
		contents.put(SidePanel.Tab.ANSWERS, new AnswersPanel(answers()));
		contents.put(SidePanel.Tab.PINNED, pinned());
		final CustomPanel custom = custom();
		contents.put(SidePanel.Tab.CUSTOM, custom);

		final SidePanel panel = new SidePanel(contents, () -> "Cmd+P", () -> {});
		panel.select(shown);
		if (editing != null)
		{
			editing.accept(custom);
		}
		if (shown == SidePanel.Tab.WORLDS)
		{
			((WorldPanel) contents.get(SidePanel.Tab.WORLDS)).highlight(302);
		}
		final BufferedImage image = paint(panel, SIDE_PANEL_HEIGHT);
		panel.stop();
		timers.reset();
		executor.shutdownNow();
		return image;
	}

	private static WorldPanel worlds()
	{
		final List<World> worlds = List.of(
			world(330, "House Party, Gilded Altar", WorldRegion.UNITED_STATES_OF_AMERICA, 1043, true),
			world(420, "Group Iron Ironman", WorldRegion.UNITED_STATES_OF_AMERICA, 812, true),
			world(301, "Trade - Free", WorldRegion.UNITED_STATES_OF_AMERICA, 1265, false),
			world(302, "Trade - Members", WorldRegion.UNITED_KINGDOM, 1843, true),
			world(307, "Wintertodt", WorldRegion.UNITED_KINGDOM, 921, true),
			world(353, "1250 skill total", WorldRegion.UNITED_STATES_OF_AMERICA, 553, true),
			world(416, "Tempoross", WorldRegion.UNITED_STATES_OF_AMERICA, 750, true),
			world(447, "", WorldRegion.GERMANY, 176, true),
			world(521, "", WorldRegion.AUSTRALIA, 193, true));
		final WorldPanel[] holder = new WorldPanel[1];
		holder[0] = new WorldPanel(() -> holder[0].show(worlds, world ->
		{
			switch (world.getId())
			{
				case 330:
				case 420:
					return WorldLocation.US_WEST;
				case 301:
				case 353:
				case 416:
					return WorldLocation.US_EAST;
				default:
					return WorldLocation.of(world.getRegion(), null);
			}
		}, id -> id == 330 || id == 420, 307, 330, true), world -> {}, (id, favourite) -> {});
		return holder[0];
	}

	private static SessionCommands.Snapshot session()
	{
		final int[] start = new int[Skill.values().length];
		final int[] now = start.clone();
		now[Skill.SLAYER.ordinal()] = 58_210;
		now[Skill.HITPOINTS.ordinal()] = 22_904;
		now[Skill.STRENGTH.ordinal()] = 41_560;
		now[Skill.DEFENCE.ordinal()] = 12_830;
		now[Skill.PRAYER.ordinal()] = 3_405;
		return new SessionCommands.Snapshot(Duration.ofMinutes(83), SessionCommands.gains(start, now));
	}

	private static Answers answers()
	{
		final Answers answers = new Answers();
		answers.add(Durations.convertTicks("250"), Icons.TIMER);
		answers.add(DryStreak.parse("1/512 600").describeDry(), Icons.DICE);
		answers.add("35m / 4.2k = " + Calculator.format(Calculator.evaluate("35m / 4.2k")), Icons.EQUALS);
		answers.add(GeTax.describe("12m"), Icons.EQUALS);
		return answers;
	}

	private static CustomPanel custom()
	{
		final List<CustomCommand> saved = List.of(
			new CustomCommand("ge", CustomCommand.Action.LINK, "https://prices.runescape.wiki/osrs/all-items?search={text}"),
			new CustomCommand("herbs", CustomCommand.Action.TIMER, "80m herb run"),
			new CustomCommand("birds", CustomCommand.Action.TIMER, "50m birdhouses"),
			new CustomCommand("loot", CustomCommand.Action.COMMAND, "panel:Loot Tracker"),
			new CustomCommand("cc", CustomCommand.Action.COPY, "Clan Chat"),
			new CustomCommand("brag", CustomCommand.Action.SHOW, "{rsn}: {kc toa} toa, {kc cox} cox"));
		final List<Command> available = List.of(new Command("panel:Loot Tracker", "Loot Tracker",
			SidebarCommands.OPEN_DETAIL, null, () -> {}));
		return new CustomPanel(() -> saved, commands -> {}, () -> available);
	}

	private static PinnedPanel pinned()
	{
		final List<Command> commands = List.of(
			new Command("panel:Loot Tracker", "Loot Tracker", SidebarCommands.OPEN_DETAIL,
				icon(PLUGINS + "loottracker/panel_icon.png"), () -> {}),
			new Command("world:previous", "Previous world", "World 302", Icons.RECENT, () -> {}),
			new Command("sidepanel:timers", "Timers", "Open in the Command Palette panel", Icons.TIMER, () -> {}),
			new Command("settings:Screen Markers", "Screen Markers", "Open settings", Icons.COG, () -> {}),
			new Command("client:screenshot", "Take screenshot", "", Icons.CAMERA, () -> {}),
			new Command("panel:Notes", "Notes", SidebarCommands.OPEN_DETAIL, icon(PLUGINS + "notes/notes_icon.png"),
				() -> {}));
		return new PinnedPanel(() -> commands, command -> {}, () -> List.of("panel:Loot Tracker", "world:previous"),
			command -> {}, () -> List.of("sidepanel:timers", "settings:Screen Markers", "client:screenshot",
			"panel:Notes"));
	}

	/**
	 * Paints the component in a window that is never shown: Swing only lays out lists that belong to one.
	 *
	 * @param height how tall to make it, or null for as tall as it likes
	 */
	private static BufferedImage paint(JComponent component, Integer height)
	{
		final JWindow window = new JWindow();
		window.setContentPane(component);
		window.pack();
		if (height != null)
		{
			window.setSize(component.getPreferredSize().width, height);
		}
		window.validate();
		layOut(component);
		final BufferedImage image = new BufferedImage(component.getWidth() * SCALE, component.getHeight() * SCALE,
			BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		g.scale(SCALE, SCALE);
		component.paint(g);
		g.dispose();
		window.dispose();
		return image;
	}

	private static void layOut(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				layOut((Container) child);
			}
		}
	}
}
