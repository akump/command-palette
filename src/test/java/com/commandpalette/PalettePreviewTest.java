package com.commandpalette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JWindow;
import net.runelite.client.ui.laf.RuneLiteLAF;
import static org.junit.Assert.assertTrue;
import org.junit.Assume;
import org.junit.Test;

/**
 * Renders the palette to build/preview.png, for eyeballing the look without starting the client.
 */
public class PalettePreviewTest
{
	private static final String[] PANELS = {"Configuration", "Loot Tracker"};

	// stand-in for the panels' own icons
	private static ImageIcon icon(int n)
	{
		final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setColor(Color.getHSBColor(n * 0.13f, 0.6f, 0.9f));
		g.fillRoundRect(1, 1, 14, 14, 4, 4);
		g.dispose();
		return new ImageIcon(image);
	}

	private static List<Command> commands()
	{
		final List<Command> commands = new ArrayList<>();
		for (int i = 0; i < PANELS.length; i++)
		{
			commands.add(new Command(PANELS[i], i == 1 ? SidebarCommands.SHOWING_DETAIL : SidebarCommands.OPEN_DETAIL,
				icon(i), () -> {}));
		}
		commands.add(new Command("Close side panel", "", Icons.CLOSE, () -> {}));
		commands.add(new Command("Hide sidebar", "", Icons.SIDEBAR, () -> {}));
		commands.add(new Command("Take screenshot", "", Icons.CAMERA, () -> {}));
		commands.add(new Command("Copy world", "To clipboard", Icons.CLIPBOARD, () -> {}));
		commands.add(new Command("Session summary", "Show in chat", Icons.CLOCK, () -> {}));
		commands.add(new Command("Open my hiscores", "Official", Icons.LINK, () -> {}));
		commands.add(new Command("Open logs folder", "", Icons.FOLDER, () -> {}));
		commands.add(new Command("Help", "Keys and commands", Icons.HELP, () -> {}));
		commands.add(new Command("Loot Tracker", "Turn plugin off", Icons.SWITCH_ON, () -> {}));
		commands.add(new Command("Twitch", "Turn plugin on", Icons.SWITCH_OFF, () -> {}));
		commands.add(new Command("Loot Tracker", "Open settings", Icons.COG, () -> {}));
		commands.add(new Command("World 302", "Hop - Members", Icons.GLOBE, () -> {}));
		commands.add(new Command("= 8,333.33", "Copy", Icons.EQUALS, () -> {}));
		commands.add(new Command("Rune platebody", "Price and alch value", Icons.of(icon(7).getImage()), () -> {}));
		commands.add(new Command("No icon at all", "", null, () -> {}));
		return commands;
	}

	private static BufferedImage render(List<Command> commands, String query, boolean showIcons)
	{
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
		panel.show(commands, typed -> TypedCommands.wikiSearch(Query.parse(typed).getText()), showIcons);
		panel.getSearch().setText(query);

		// Swing only lays out the rows of a list that belongs to a window, which needn't be shown
		final JWindow window = new JWindow();
		window.setContentPane(panel);
		window.pack();
		final BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		panel.paint(g);
		g.dispose();
		window.dispose();
		return image;
	}

	@Test
	public void renderPreview() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		RuneLiteLAF.setup();

		// the list shows twelve rows at a time
		final List<Command> commands = commands();
		final BufferedImage all = render(commands.subList(0, 12), "", true);
		final BufferedImage filtered = render(commands.subList(12, commands.size()), "", true);
		final BufferedImage none = render(commands.subList(0, 6), "", false);

		final int gap = 20;
		final BufferedImage image = new BufferedImage(all.getWidth() * 3 + gap * 4,
			all.getHeight() + gap * 2, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setColor(new Color(62, 53, 41));
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.drawImage(all, gap, gap, null);
		g.drawImage(filtered, all.getWidth() + gap * 2, gap, null);
		g.drawImage(none, all.getWidth() * 2 + gap * 3, gap, null);
		g.dispose();

		final File out = new File("build/preview.png");
		out.getParentFile().mkdirs();
		ImageIO.write(image, "png", out);

		assertTrue(none.getHeight() < all.getHeight());
	}
}
