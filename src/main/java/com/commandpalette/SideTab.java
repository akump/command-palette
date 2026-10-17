package com.commandpalette;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * One tab of the {@link SidePanel}.
 */
abstract class SideTab extends JPanel
{
	/**
	 * How wide a tab's text can be: the panel's width inside its border, less a scroll bar in case there is one.
	 */
	static final int TEXT_WIDTH = PluginPanel.PANEL_WIDTH - 2 * PluginPanel.BORDER_OFFSET;
	/**
	 * How wide text can be in a {@link #row()} that has an icon button at its end.
	 */
	static final int ROW_TEXT_WIDTH = TEXT_WIDTH - 2 * 8 - 6 - (Icons.SIZE + 4);

	SideTab()
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
	}

	/**
	 * Show things as they are now. Called on the Swing thread when the tab is shown, and every second while it is
	 * if it is {@link #isLive()}.
	 */
	abstract void refresh();

	/**
	 * Whether what the tab shows changes by itself, as a countdown does.
	 */
	boolean isLive()
	{
		return false;
	}

	// shared looks

	static JLabel heading(String text)
	{
		final JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		label.setFont(FontManager.getRunescapeBoldFont());
		return label;
	}

	/**
	 * Small grey text that wraps to the panel's width.
	 */
	static JLabel note(String text)
	{
		final JLabel label = new Wrapping("<html>" + text + "</html>", TEXT_WIDTH);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	/**
	 * Lets the content scroll up and down, never sideways: it is kept to the panel's width, and anything too long
	 * for it, such as a long name, is cut short with "..." rather than pushing what is beside it out of sight.
	 */
	static JScrollPane scrolling(JComponent content)
	{
		final WidthTracking wrapper = new WidthTracking();
		wrapper.add(content, BorderLayout.NORTH);
		final JScrollPane scroll = new JScrollPane(wrapper);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		return scroll;
	}

	/**
	 * A box for one entry of a list, the way RuneLite's own panels draw them.
	 */
	static JPanel row()
	{
		final JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		return row;
	}

	/**
	 * Text to show as HTML, which is how it wraps, with the characters HTML gives a meaning to made plain.
	 */
	static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	static Font small()
	{
		return FontManager.getRunescapeSmallFont();
	}

	/**
	 * A label whose HTML wraps at a width, and which asks for the height that takes. A plain HTML label asks for
	 * its whole text on one line, which widens whatever holds it.
	 */
	static final class Wrapping extends JLabel
	{
		private final int width;

		/**
		 * @param width the label's width, its icon and border included
		 */
		Wrapping(String text, int width)
		{
			super(text);
			this.width = width;
		}

		@Override
		public Dimension getPreferredSize()
		{
			final View view = (View) getClientProperty(BasicHTML.propertyKey);
			if (view == null || isPreferredSizeSet())
			{
				return super.getPreferredSize();
			}
			final Insets insets = getInsets();
			final int icon = getIcon() == null ? 0 : getIcon().getIconWidth() + getIconTextGap();
			view.setSize(Math.max(1, width - insets.left - insets.right - icon), 0);
			final int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
			return new Dimension(width,
				Math.max(height, getIcon() == null ? 0 : getIcon().getIconHeight()) + insets.top + insets.bottom);
		}
	}

	// takes the viewport's width, so that nothing in it is laid out wider
	private static final class WidthTracking extends JPanel implements Scrollable
	{
		private WidthTracking()
		{
			super(new BorderLayout());
			setOpaque(false);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction)
		{
			return Math.max(16, visible.height - 16);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
