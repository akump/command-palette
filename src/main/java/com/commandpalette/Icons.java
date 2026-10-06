package com.commandpalette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import net.runelite.client.ui.ColorScheme;

/**
 * Icons for the commands that have no picture of their own. Each is drawn when painted rather than loaded from
 * an image, on a 64 unit square scaled down to the 16 pixel icon, so they stay sharp on high density screens.
 */
enum Icons implements Icon
{
	CLOSE
		{
			@Override
			void draw(Graphics2D g)
			{
				line(g, 18, 18, 46, 46);
				line(g, 46, 18, 18, 46);
			}
		},
	SIDEBAR
		{
			@Override
			void draw(Graphics2D g)
			{
				g.draw(new RoundRectangle2D.Double(7, 12, 50, 40, 8, 8));
				g.fill(new RoundRectangle2D.Double(38, 12, 19, 40, 8, 8));
			}
		},
	CAMERA
		{
			@Override
			void draw(Graphics2D g)
			{
				g.draw(new RoundRectangle2D.Double(7, 19, 50, 34, 8, 8));
				g.fill(new RoundRectangle2D.Double(23, 10, 18, 10, 4, 4));
				g.draw(new Ellipse2D.Double(23, 27, 18, 18));
			}
		},
	CLIPBOARD
		{
			@Override
			void draw(Graphics2D g)
			{
				g.draw(new RoundRectangle2D.Double(14, 13, 36, 44, 6, 6));
				g.fill(new RoundRectangle2D.Double(23, 6, 18, 13, 4, 4));
				line(g, 23, 32, 41, 32);
				line(g, 23, 44, 41, 44);
			}
		},
	CLOCK
		{
			@Override
			void draw(Graphics2D g)
			{
				g.draw(new Ellipse2D.Double(8, 8, 48, 48));
				line(g, 32, 32, 32, 18);
				line(g, 32, 32, 42, 39);
			}
		},
	LINK
		{
			@Override
			void draw(Graphics2D g)
			{
				final Path2D box = new Path2D.Double();
				box.moveTo(30, 16);
				box.lineTo(12, 16);
				box.lineTo(12, 52);
				box.lineTo(48, 52);
				box.lineTo(48, 34);
				g.draw(box);
				line(g, 30, 34, 53, 11);
				final Path2D head = new Path2D.Double();
				head.moveTo(39, 11);
				head.lineTo(53, 11);
				head.lineTo(53, 25);
				g.draw(head);
			}
		},
	FOLDER
		{
			@Override
			void draw(Graphics2D g)
			{
				final Path2D folder = new Path2D.Double();
				folder.moveTo(8, 16);
				folder.lineTo(25, 16);
				folder.lineTo(31, 23);
				folder.lineTo(56, 23);
				folder.lineTo(56, 51);
				folder.lineTo(8, 51);
				folder.closePath();
				g.draw(folder);
			}
		},
	HELP
		{
			@Override
			void draw(Graphics2D g)
			{
				g.draw(new Ellipse2D.Double(8, 8, 48, 48));
				text(g, "?", 36);
			}
		},
	SWITCH_ON
		{
			@Override
			void draw(Graphics2D g)
			{
				g.setColor(ColorScheme.BRAND_ORANGE);
				g.fill(new RoundRectangle2D.Double(3, 15, 58, 34, 34, 34));
				g.setColor(Color.WHITE);
				g.fill(new Ellipse2D.Double(32, 20, 24, 24));
			}
		},
	SWITCH_OFF
		{
			@Override
			void draw(Graphics2D g)
			{
				g.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
				g.fill(new RoundRectangle2D.Double(3, 15, 58, 34, 34, 34));
				g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				g.fill(new Ellipse2D.Double(8, 20, 24, 24));
			}
		},
	COG
		{
			@Override
			void draw(Graphics2D g)
			{
				g.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				for (int tooth = 0; tooth < 8; tooth++)
				{
					final double angle = tooth * Math.PI / 4;
					line(g, 32 + 17 * Math.cos(angle), 32 + 17 * Math.sin(angle),
						32 + 23 * Math.cos(angle), 32 + 23 * Math.sin(angle));
				}
				g.setStroke(new BasicStroke(10));
				g.draw(new Ellipse2D.Double(19, 19, 26, 26));
			}
		},
	GLOBE
		{
			@Override
			void draw(Graphics2D g)
			{
				g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(new Ellipse2D.Double(8, 8, 48, 48));
				g.draw(new Ellipse2D.Double(22, 8, 20, 48));
				line(g, 8, 32, 56, 32);
				line(g, 13, 20, 51, 20);
				line(g, 13, 44, 51, 44);
			}
		},
	DICE
		{
			@Override
			void draw(Graphics2D g)
			{
				g.draw(new RoundRectangle2D.Double(10, 10, 44, 44, 12, 12));
				g.fill(new Ellipse2D.Double(17, 17, 9, 9));
				g.fill(new Ellipse2D.Double(27.5, 27.5, 9, 9));
				g.fill(new Ellipse2D.Double(38, 38, 9, 9));
			}
		},
	EQUALS
		{
			@Override
			void draw(Graphics2D g)
			{
				g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				line(g, 14, 24, 50, 24);
				line(g, 14, 41, 50, 41);
			}
		},
	SEARCH
		{
			@Override
			void draw(Graphics2D g)
			{
				g.draw(new Ellipse2D.Double(9, 9, 32, 32));
				g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				line(g, 40, 40, 54, 54);
			}
		};

	static final int SIZE = 16;

	private static final int UNITS = 64;

	/**
	 * Draw the icon within 64 by 64, in the colour and the round 6 unit line already set.
	 */
	abstract void draw(Graphics2D g);

	@Override
	public void paintIcon(Component c, Graphics graphics, int x, int y)
	{
		final Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.translate(x, y);
		g.scale((double) SIZE / UNITS, (double) SIZE / UNITS);
		g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		draw(g);
		g.dispose();
	}

	@Override
	public int getIconWidth()
	{
		return SIZE;
	}

	@Override
	public int getIconHeight()
	{
		return SIZE;
	}

	/**
	 * An icon of the image shrunk to fit, whatever its size. It is drawn from the image each time, so one that is
	 * still loading shows up once it has and the list is repainted.
	 */
	static Icon of(Image image)
	{
		return new Icon()
		{
			@Override
			public void paintIcon(Component c, Graphics graphics, int x, int y)
			{
				final int width = image.getWidth(null);
				final int height = image.getHeight(null);
				if (width <= 0 || height <= 0)
				{
					return;
				}
				final double scale = Math.min(1, Math.min((double) SIZE / width, (double) SIZE / height));
				final int w = (int) Math.round(width * scale);
				final int h = (int) Math.round(height * scale);
				final Graphics2D g = (Graphics2D) graphics.create();
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(image, x + (SIZE - w) / 2, y + (SIZE - h) / 2, w, h, null);
				g.dispose();
			}

			@Override
			public int getIconWidth()
			{
				return SIZE;
			}

			@Override
			public int getIconHeight()
			{
				return SIZE;
			}
		};
	}

	private static void line(Graphics2D g, double x1, double y1, double x2, double y2)
	{
		g.draw(new Line2D.Double(x1, y1, x2, y2));
	}

	// centred in the square
	private static void text(Graphics2D g, String text, int size)
	{
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
		final FontMetrics metrics = g.getFontMetrics();
		g.drawString(text, (UNITS - metrics.stringWidth(text)) / 2f,
			(UNITS - metrics.getHeight()) / 2f + metrics.getAscent());
	}
}
