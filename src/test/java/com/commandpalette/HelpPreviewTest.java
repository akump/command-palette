package com.commandpalette;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.laf.RuneLiteLAF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Assume;
import org.junit.Test;

public class HelpPreviewTest
{
	private static CommandPaletteConfig config(boolean worlds)
	{
		return new CommandPaletteConfig()
		{
			@Override
			public Keybind hotkey()
			{
				return new Keybind(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK);
			}

			@Override
			public boolean worldCommands()
			{
				return worlds;
			}
		};
	}

	@Test
	public void listsTheHotkeyAndOnlyTheGroupsThatAreOn()
	{
		final String all = new HelpCommand(config(true)).text();
		assertTrue(all.contains("Ctrl+P"));
		assertEquals("Shift+Cmd+F1", HelpCommand.describe(
			new Keybind(KeyEvent.VK_F1, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
		assertEquals("Not set", HelpCommand.describe(Keybind.NOT_SET));
		assertTrue(all.contains("world 302"));
		assertFalse(new HelpCommand(config(false)).text().contains("world 302"));
	}

	/**
	 * Renders the help text to build/help.png.
	 */
	@Test
	public void renderPreview() throws Exception
	{
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		RuneLiteLAF.setup();

		final JLabel label = new JLabel(new HelpCommand(config(true)).text());
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		label.setOpaque(true);
		final JWindow window = new JWindow();
		window.setContentPane(label);
		window.pack();
		final BufferedImage image = new BufferedImage(label.getWidth(), label.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		label.paint(g);
		g.dispose();
		window.dispose();

		final File out = new File("build/help.png");
		out.getParentFile().mkdirs();
		ImageIO.write(image, "png", out);
	}
}
