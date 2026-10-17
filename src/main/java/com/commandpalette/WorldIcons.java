package com.commandpalette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;

/**
 * A world's icon: the flag of where it is, with a star in the corner, gold for a members' world and silver for a
 * free one, as the game's own world switcher marks them. A US world's flag has an arrow beside it for its coast, as
 * in that switcher: on red pointing right for the east coast, on blue pointing left for the west.
 * Drawn like {@link Icons}, on a 64 unit square.
 */
final class WorldIcons
{
	// the flag's box within the square, as wide as it and two thirds as tall
	private static final double TOP = 10;
	private static final double HEIGHT = 43;
	private static final double WIDTH = 64;

	private static final Color MEMBERS_STAR = new Color(0xF5C542);
	private static final Color FREE_STAR = new Color(0xC8C8C8);
	private static final Color STAR_OUTLINE = new Color(0x1E1E1E);
	private static final Color FLAG_OUTLINE = new Color(0, 0, 0, 110);

	private static final Color RED = new Color(0xC8102E);
	private static final Color WHITE = Color.WHITE;
	private static final Color NAVY = new Color(0x012169);
	private static final Color US_BLUE = new Color(0x3C3B6E);
	private static final Color EAST_RED = new Color(0xB22234);
	// the block beside a US flag that holds the arrow for its coast
	private static final double COAST_WIDTH = 20;

	private static final Map<String, Icon> CACHE = new HashMap<>();

	private WorldIcons()
	{
	}

	/**
	 * @param location where the world is, or null if unknown, which shows a globe instead of a flag
	 */
	static synchronized Icon of(WorldLocation location, boolean members)
	{
		return CACHE.computeIfAbsent(location + ":" + members, key -> new WorldIcon(location, members));
	}

	private static final class WorldIcon implements Icon
	{
		private final WorldLocation location;
		private final boolean members;

		private WorldIcon(WorldLocation location, boolean members)
		{
			this.location = location;
			this.members = members;
		}

		@Override
		public void paintIcon(Component c, Graphics graphics, int x, int y)
		{
			if (location == null)
			{
				Icons.GLOBE.paintIcon(c, graphics, x, y);
			}
			final Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.translate(x, y);
			g.scale(Icons.SIZE / 64.0, Icons.SIZE / 64.0);
			if (location != null)
			{
				final Shape box = new RoundRectangle2D.Double(0, TOP, WIDTH, HEIGHT, 8, 8);
				final Graphics2D flag = (Graphics2D) g.create();
				flag.clip(box);
				flag.translate(0, TOP);
				drawFlag(flag, location);
				flag.dispose();
				g.setColor(FLAG_OUTLINE);
				g.setStroke(new BasicStroke(2));
				g.draw(box);
			}
			drawStar(g, members ? MEMBERS_STAR : FREE_STAR);
			g.dispose();
		}

		@Override
		public int getIconWidth()
		{
			return Icons.SIZE;
		}

		@Override
		public int getIconHeight()
		{
			return Icons.SIZE;
		}
	}

	// in the bottom right corner, over the flag, with a dark edge to stand out from it
	private static void drawStar(Graphics2D g, Color color)
	{
		final Shape star = star(51, 48, 13, 5.5);
		g.setColor(STAR_OUTLINE);
		g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(star);
		g.setColor(color);
		g.fill(star);
	}

	private static Shape star(double cx, double cy, double outer, double inner)
	{
		final Path2D star = new Path2D.Double();
		for (int point = 0; point < 10; point++)
		{
			final double radius = point % 2 == 0 ? outer : inner;
			final double angle = -Math.PI / 2 + point * Math.PI / 5;
			final double px = cx + radius * Math.cos(angle);
			final double py = cy + radius * Math.sin(angle);
			if (point == 0)
			{
				star.moveTo(px, py);
			}
			else
			{
				star.lineTo(px, py);
			}
		}
		star.closePath();
		return star;
	}

	/**
	 * The flag, simplified to what reads at 16 pixels, filling 64 by 43 from the origin.
	 */
	static void drawFlag(Graphics2D g, WorldLocation location)
	{
		switch (location)
		{
			case US:
				usFlag(g, 0, WIDTH);
				break;
			case US_EAST:
				usFlag(g, 0, WIDTH - COAST_WIDTH);
				fill(g, EAST_RED, WIDTH - COAST_WIDTH, 0, COAST_WIDTH, HEIGHT);
				// high in its block, clear of the star in the corner
				arrow(g, WIDTH - COAST_WIDTH / 2, 15, true);
				break;
			case US_WEST:
				fill(g, US_BLUE, 0, 0, COAST_WIDTH, HEIGHT);
				arrow(g, COAST_WIDTH / 2, HEIGHT / 2, false);
				usFlag(g, COAST_WIDTH, WIDTH - COAST_WIDTH);
				break;
			case UK:
				unionJack(g, WIDTH, HEIGHT);
				break;
			case AUSTRALIA:
				fill(g, NAVY, 0, 0, WIDTH, HEIGHT);
				unionJack(g, 32, 21.5);
				g.setColor(WHITE);
				// the Commonwealth Star, and the Southern Cross
				g.fill(star(16, 32, 6, 2.6));
				g.fill(star(48, 9, 3.5, 1.5));
				g.fill(star(41, 22, 3.5, 1.5));
				g.fill(star(55, 19, 3.5, 1.5));
				g.fill(star(48, 36, 3.5, 1.5));
				break;
			case GERMANY:
				fill(g, Color.BLACK, 0, 0, WIDTH, HEIGHT / 3);
				fill(g, new Color(0xDD0000), 0, HEIGHT / 3, WIDTH, HEIGHT / 3);
				fill(g, new Color(0xFFCE00), 0, 2 * HEIGHT / 3, WIDTH, HEIGHT / 3);
				break;
			case BRAZIL:
				fill(g, new Color(0x009C3B), 0, 0, WIDTH, HEIGHT);
				final Path2D diamond = new Path2D.Double();
				diamond.moveTo(WIDTH / 2, 5);
				diamond.lineTo(WIDTH - 6, HEIGHT / 2);
				diamond.lineTo(WIDTH / 2, HEIGHT - 5);
				diamond.lineTo(6, HEIGHT / 2);
				diamond.closePath();
				g.setColor(new Color(0xFFDF00));
				g.fill(diamond);
				g.setColor(new Color(0x002776));
				g.fill(new Ellipse2D.Double(WIDTH / 2 - 10, HEIGHT / 2 - 10, 20, 20));
				break;
			case JAPAN:
				fill(g, WHITE, 0, 0, WIDTH, HEIGHT);
				g.setColor(new Color(0xBC002D));
				g.fill(new Ellipse2D.Double(WIDTH / 2 - 13, HEIGHT / 2 - 13, 26, 26));
				break;
			case SINGAPORE:
				fill(g, new Color(0xEF3340), 0, 0, WIDTH, HEIGHT / 2);
				fill(g, WHITE, 0, HEIGHT / 2, WIDTH, HEIGHT / 2);
				// the crescent: a white disc with a red one taken out of it
				final Area crescent = new Area(new Ellipse2D.Double(5, 3, 16, 16));
				crescent.subtract(new Area(new Ellipse2D.Double(9, 3, 16, 16)));
				g.setColor(WHITE);
				g.fill(crescent);
				break;
			case SOUTH_AFRICA:
				southAfrica(g);
				break;
			default:
				fill(g, new Color(0x5A5A5A), 0, 0, WIDTH, HEIGHT);
				break;
		}
	}

	// the stars and stripes, from x across that width
	private static void usFlag(Graphics2D g, double x, double width)
	{
		fill(g, WHITE, x, 0, width, HEIGHT);
		for (int stripe = 0; stripe < 13; stripe += 2)
		{
			fill(g, RED, x, stripe * HEIGHT / 13, width, HEIGHT / 13);
		}
		fill(g, US_BLUE, x, 0, width * 0.44, 7 * HEIGHT / 13);
	}

	// white, centred on the point, pointing right or left
	private static void arrow(Graphics2D g, double cx, double cy, boolean right)
	{
		final double half = 8;
		final double head = 7;
		final double shaft = 3;
		final double direction = right ? 1 : -1;
		final Path2D arrow = new Path2D.Double();
		arrow.moveTo(cx - direction * half, cy - shaft);
		arrow.lineTo(cx + direction * (half - head), cy - shaft);
		arrow.lineTo(cx + direction * (half - head), cy - head);
		arrow.lineTo(cx + direction * half, cy);
		arrow.lineTo(cx + direction * (half - head), cy + head);
		arrow.lineTo(cx + direction * (half - head), cy + shaft);
		arrow.lineTo(cx - direction * half, cy + shaft);
		arrow.closePath();
		g.setColor(WHITE);
		g.fill(arrow);
	}

	private static void unionJack(Graphics2D g, double width, double height)
	{
		fill(g, NAVY, 0, 0, width, height);
		final double scale = height / HEIGHT;
		g.setStroke(new BasicStroke((float) (10 * scale)));
		g.setColor(WHITE);
		g.draw(new Line2D.Double(0, 0, width, height));
		g.draw(new Line2D.Double(width, 0, 0, height));
		g.setStroke(new BasicStroke((float) (4 * scale)));
		g.setColor(RED);
		g.draw(new Line2D.Double(0, 0, width, height));
		g.draw(new Line2D.Double(width, 0, 0, height));
		fill(g, WHITE, 0, height / 2 - 7 * scale, width, 14 * scale);
		fill(g, WHITE, width / 2 - 7 * scale, 0, 14 * scale, height);
		fill(g, RED, 0, height / 2 - 4 * scale, width, 8 * scale);
		fill(g, RED, width / 2 - 4 * scale, 0, 8 * scale, height);
	}

	private static void southAfrica(Graphics2D g)
	{
		fill(g, new Color(0xE03C31), 0, 0, WIDTH, HEIGHT / 2);
		fill(g, new Color(0x001489), 0, HEIGHT / 2, WIDTH, HEIGHT / 2);
		// the Y on its side, white edged, around a black triangle edged in gold
		g.setColor(WHITE);
		g.fill(sidewaysY(18));
		g.setColor(new Color(0x007749));
		g.fill(sidewaysY(10));
		final Path2D gold = new Path2D.Double();
		gold.moveTo(0, 5);
		gold.lineTo(22, HEIGHT / 2);
		gold.lineTo(0, HEIGHT - 5);
		gold.closePath();
		g.setColor(new Color(0xFFB81C));
		g.fill(gold);
		final Path2D black = new Path2D.Double();
		black.moveTo(0, 10);
		black.lineTo(16, HEIGHT / 2);
		black.lineTo(0, HEIGHT - 10);
		black.closePath();
		g.setColor(Color.BLACK);
		g.fill(black);
	}

	// a band from both left corners to the middle, then on to the right edge
	private static Shape sidewaysY(double thickness)
	{
		final Path2D y = new Path2D.Double();
		y.moveTo(0, 0);
		y.lineTo(26, HEIGHT / 2);
		y.lineTo(WIDTH, HEIGHT / 2);
		y.moveTo(0, HEIGHT);
		y.lineTo(26, HEIGHT / 2);
		return new BasicStroke((float) thickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER)
			.createStrokedShape(y);
	}

	private static void fill(Graphics2D g, Color color, double x, double y, double width, double height)
	{
		g.setColor(color);
		g.fill(new Rectangle2D.Double(x, y, width, height));
	}
}
