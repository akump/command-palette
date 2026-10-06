package com.commandpalette;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.Icon;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import net.runelite.api.Experience;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.laf.RuneLiteLAF;
import net.runelite.client.util.QuantityFormatter;
import static org.junit.Assert.assertTrue;
import org.junit.Assume;
import org.junit.Test;

/**
 * Types a few searches into the palette and writes the frames to build/demo.gif, which the README's copy in
 * docs/ is made from. The commands are stand-ins for the ones a running client would list.
 */
public class DemoGifTest
{
	// drawn at twice the size so that the README's copy stays sharp on high density screens
	private static final int SCALE = 2;
	private static final int MARGIN = 10;
	private static final int KEY_MS = 90;
	private static final int PAUSE_MS = 1300;

	private static final String PLUGINS = "/net/runelite/client/plugins/";
	private static final int SLAYER_XP = 5_000_000;

	private final List<BufferedImage> frames = new ArrayList<>();
	private final List<Integer> delays = new ArrayList<>();

	private PalettePanel panel;
	private JWindow window;
	private int width;
	private int height;

	private static Icon icon(String resource) throws Exception
	{
		return Icons.of(ImageIO.read(DemoGifTest.class.getResourceAsStream(resource)));
	}

	private static Command command(String key, String title, String detail, Icon icon)
	{
		return new Command(key, title, detail, icon, () -> {});
	}

	private static List<Command> commands() throws Exception
	{
		final List<Command> commands = new ArrayList<>();
		final String[][] panels = {
			{"Configuration", "config/config_icon.png"},
			{"Loot Tracker", "loottracker/panel_icon.png"},
			{"Grand Exchange", "grandexchange/ge_icon.png"},
			{"World Switcher", "worldhopper/icon.png"},
			{"Screen Markers", "screenmarkers/panel_icon.png"},
			{"Notes", "notes/notes_icon.png"},
			{"Info", "info/info_icon.png"},
		};
		for (String[] p : panels)
		{
			commands.add(command("panel:" + p[0], p[0], SidebarCommands.OPEN_DETAIL, icon(PLUGINS + p[1])));
		}
		commands.add(command("sidebar:toggle", "Hide sidebar", "", Icons.SIDEBAR));
		commands.add(command("screenshot", "Take screenshot", "", Icons.CAMERA));
		commands.add(command("copy:world", "Copy world", "To clipboard", Icons.CLIPBOARD));
		commands.add(command("copy:coordinates", "Copy coordinates", "To clipboard", Icons.CLIPBOARD));
		commands.add(command("session", "Session summary", "Show in chat", Icons.CLOCK));
		commands.add(command("link:hiscores", "Open my hiscores", "Official", Icons.LINK));
		commands.add(command("folder:screenshots", "Open screenshots folder", "", Icons.FOLDER));
		for (String plugin : new String[]{"Loot Tracker", "Screen Markers", "Twitch"})
		{
			commands.add(command(PluginCommands.SETTINGS_KIND + plugin, plugin, "Open settings", Icons.COG));
		}
		commands.add(command(WorldCommands.KIND + 301, "World 301", "Hop - Free - Trade", Icons.GLOBE));
		commands.add(command(WorldCommands.KIND + 302, "World 302", "Hop - Members - Trade", Icons.GLOBE));
		commands.add(command(WorldCommands.KIND + 307, "World 307", "Hop - Members - Wintertodt", Icons.GLOBE));
		commands.add(command(WorldCommands.KIND + 309, "World 309", "Hop - Members - Wintertodt", Icons.GLOBE));
		commands.add(command("help", "Help", "Keys and commands", Icons.HELP));
		commands.add(command(PluginCommands.KIND + "Loot Tracker", "Loot Tracker", "Turn plugin off", Icons.SWITCH_ON));
		commands.add(command(PluginCommands.KIND + "Screen Markers", "Screen Markers", "Turn plugin off", Icons.SWITCH_ON));
		commands.add(command(PluginCommands.KIND + "Twitch", "Twitch", "Turn plugin on", Icons.SWITCH_OFF));
		return commands;
	}

	// the sum and the wiki search are the plugin's own; the xp needs a logged in client, so it is worked out here
	private static List<Command> forQuery(String typed, Icon slayer)
	{
		final Query query = Query.parse(typed);
		if (query.isWiki())
		{
			return TypedCommands.wikiSearch(query.getText());
		}
		if (query.hasPrefix() || query.getText().isEmpty())
		{
			return new ArrayList<>();
		}

		final List<Command> commands = new ArrayList<>();
		final Double value = Calculator.evaluate(query.getText());
		if (value != null)
		{
			commands.add(new Command("= " + Calculator.format(value), "Copy", Icons.EQUALS, () -> {}));
		}
		if (query.getText().equalsIgnoreCase("xp 92 slayer"))
		{
			final int remaining = Experience.getXpForLevel(92) - SLAYER_XP;
			commands.add(new Command("Slayer: " + QuantityFormatter.formatNumber(remaining) + " xp to level 92",
				"Show in chat", slayer, () -> {}));
		}
		commands.addAll(TypedCommands.wikiSearch(query.getText()));
		return commands;
	}

	private void frame(int ms)
	{
		window.pack();
		final BufferedImage image = new BufferedImage(width * SCALE, height * SCALE, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		g.setColor(ColorScheme.DARK_GRAY_COLOR);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.scale(SCALE, SCALE);
		g.translate(MARGIN, MARGIN);
		panel.paint(g);
		g.dispose();
		frames.add(image);
		delays.add(ms);
	}

	private void type(String text)
	{
		final JTextField search = panel.getSearch();
		for (int i = 1; i <= text.length(); i++)
		{
			search.setText(text.substring(0, i));
			frame(i == text.length() ? PAUSE_MS : KEY_MS);
		}
	}

	private void clear()
	{
		final JTextField search = panel.getSearch();
		while (!search.getText().isEmpty())
		{
			search.setText(search.getText().substring(0, search.getText().length() - 1));
			frame(search.getText().isEmpty() ? PAUSE_MS / 2 : KEY_MS / 2);
		}
	}

	private void press(int key, int times)
	{
		final JTextField search = panel.getSearch();
		for (int i = 0; i < times; i++)
		{
			// a field without the focus isn't sent key events, so its listeners are handed them
			final KeyEvent event = new KeyEvent(search, KeyEvent.KEY_PRESSED, 0, 0, key, KeyEvent.CHAR_UNDEFINED);
			for (KeyListener listener : search.getKeyListeners())
			{
				listener.keyPressed(event);
			}
			frame(i == times - 1 ? PAUSE_MS : KEY_MS * 3);
		}
	}

	@Test
	public void renderDemo() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		RuneLiteLAF.setup();
		final Icon slayer = icon("/skill_icons_small/slayer.png");
		final List<Command> commands = commands();
		// the caret blinks on the Swing thread, so the typing has to happen there too
		SwingUtilities.invokeAndWait(() -> script(commands, slayer));

		final File out = new File("build/demo.gif");
		out.getParentFile().mkdirs();
		write(out);
		assertTrue(out.length() > 0);
	}

	private void script(List<Command> commands, Icon slayer)
	{
		panel = new PalettePanel(new PalettePanel.Listener()
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
		panel.show(commands, typed -> forQuery(typed, slayer), true);
		window = new JWindow();
		window.setContentPane(panel);
		window.pack();
		// the list is at its tallest with nothing typed, and every frame is that size
		width = panel.getWidth() + MARGIN * 2;
		height = panel.getHeight() + MARGIN * 2;
		// the field hasn't the focus, which is what would otherwise show its caret
		panel.getSearch().getCaret().setVisible(true);

		frame(PAUSE_MS);
		press(KeyEvent.VK_DOWN, 3);
		type("loot");
		press(KeyEvent.VK_DOWN, 2);
		clear();
		type("hop wint");
		clear();
		type(">scr");
		clear();
		type("35m / 4.2k");
		clear();
		type("xp 92 slayer");
		clear();
		type("?abyssal whip");
		clear();
		window.dispose();
	}

	// each frame after the first holds only the rectangle that changed, drawn over the one before
	private void write(File out) throws Exception
	{
		final ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
		try (ImageOutputStream stream = ImageIO.createImageOutputStream(out))
		{
			writer.setOutput(stream);
			writer.prepareWriteSequence(null);
			BufferedImage previous = null;
			for (int i = 0; i < frames.size(); i++)
			{
				final BufferedImage image = frames.get(i);
				final Rectangle changed = previous == null
					? new Rectangle(0, 0, image.getWidth(), image.getHeight()) : changed(previous, image);
				final BufferedImage part = image.getSubimage(changed.x, changed.y, changed.width, changed.height);
				writer.writeToSequence(new IIOImage(part, null, metadata(writer, part, changed, delays.get(i))), null);
				previous = image;
			}
			writer.endWriteSequence();
		}
		finally
		{
			writer.dispose();
		}
	}

	private static Rectangle changed(BufferedImage before, BufferedImage after)
	{
		int left = after.getWidth();
		int top = after.getHeight();
		int right = -1;
		int bottom = -1;
		for (int y = 0; y < after.getHeight(); y++)
		{
			for (int x = 0; x < after.getWidth(); x++)
			{
				if (before.getRGB(x, y) != after.getRGB(x, y))
				{
					left = Math.min(left, x);
					top = Math.min(top, y);
					right = Math.max(right, x);
					bottom = Math.max(bottom, y);
				}
			}
		}
		return right < 0 ? new Rectangle(0, 0, 1, 1) : new Rectangle(left, top, right - left + 1, bottom - top + 1);
	}

	private static IIOMetadata metadata(ImageWriter writer, BufferedImage image, Rectangle at, int ms) throws Exception
	{
		final IIOMetadata metadata = writer.getDefaultImageMetadata(
			ImageTypeSpecifier.createFromRenderedImage(image), null);
		final String format = metadata.getNativeMetadataFormatName();
		final IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);

		final IIOMetadataNode descriptor = child(root, "ImageDescriptor");
		descriptor.setAttribute("imageLeftPosition", Integer.toString(at.x));
		descriptor.setAttribute("imageTopPosition", Integer.toString(at.y));

		final IIOMetadataNode control = child(root, "GraphicControlExtension");
		control.setAttribute("disposalMethod", "doNotDispose");
		control.setAttribute("userInputFlag", "FALSE");
		control.setAttribute("transparentColorFlag", "FALSE");
		control.setAttribute("transparentColorIndex", "0");
		// in hundredths of a second
		control.setAttribute("delayTime", Integer.toString(ms / 10));

		final IIOMetadataNode application = new IIOMetadataNode("ApplicationExtension");
		application.setAttribute("applicationID", "NETSCAPE");
		application.setAttribute("authenticationCode", "2.0");
		// repeat for ever
		application.setUserObject(new byte[]{1, 0, 0});
		child(root, "ApplicationExtensions").appendChild(application);

		metadata.setFromTree(format, root);
		return metadata;
	}

	private static IIOMetadataNode child(IIOMetadataNode root, String name)
	{
		for (int i = 0; i < root.getLength(); i++)
		{
			if (root.item(i).getNodeName().equals(name))
			{
				return (IIOMetadataNode) root.item(i);
			}
		}
		final IIOMetadataNode node = new IIOMetadataNode(name);
		root.appendChild(node);
		return node;
	}
}
