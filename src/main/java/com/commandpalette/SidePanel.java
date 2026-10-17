package com.commandpalette;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * The plugin's side panel: the palette's commands that are better kept on screen, one tab each.
 */
class SidePanel extends PluginPanel
{
	// the panel's tooltip in the sidebar, which the palette finds it by
	static final String NAME = "Command Palette";

	enum Tab
	{
		WORLDS("Worlds", Icons.GLOBE),
		TIMERS("Timers", Icons.TIMER),
		SESSION("Session", Icons.CLOCK),
		ANSWERS("Answers", Icons.EQUALS),
		PINNED("Pinned", Icons.PIN),
		CUSTOM("Custom", Icons.CUSTOM);

		private final String title;
		private final Icons icon;

		Tab(String title, Icons icon)
		{
			this.title = title;
			this.icon = icon;
		}

		String getTitle()
		{
			return title;
		}

		Icons getIcon()
		{
			return icon;
		}
	}

	private static final int TICK_MILLIS = 1000;

	private final Map<Tab, SideTab> contents;
	private final Map<Tab, MaterialTab> tabs = new EnumMap<>(Tab.class);
	private final MaterialTabGroup group;
	private final Supplier<String> hotkey;
	private final JLabel help;
	// counts down and updates the session while they are on screen
	private final Timer ticker = new Timer(TICK_MILLIS, e -> tick());
	private Tab selected = Tab.WORLDS;

	/**
	 * @param contents what each tab shows, in the order of {@link Tab}
	 * @param hotkey   the hotkey that opens the palette, "Ctrl+P", or null if it has no name to show
	 * @param showHelp shows the help command's window
	 */
	SidePanel(Map<Tab, SideTab> contents, Supplier<String> hotkey, Runnable showHelp)
	{
		super(false);
		this.contents = new EnumMap<>(contents);
		this.hotkey = hotkey;

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET));

		final JPanel display = new JPanel(new BorderLayout());
		display.setOpaque(false);
		group = new MaterialTabGroup(display);
		group.setLayout(new GridLayout(1, Tab.values().length, 4, 0));
		for (Tab tab : Tab.values())
		{
			final MaterialTab materialTab = new MaterialTab(new ImageIcon(tab.getIcon().image()), group,
				this.contents.get(tab));
			materialTab.setToolTipText(tab.getTitle());
			materialTab.setOnSelectEvent(() ->
			{
				selected = tab;
				this.contents.get(tab).refresh();
				return true;
			});
			group.addTab(materialTab);
			tabs.put(tab, materialTab);
		}
		group.select(tabs.get(Tab.WORLDS));

		// how to find out what else the palette can do, below every tab
		final JButton helpButton = new JButton("Help");
		helpButton.setFont(SideTab.small());
		helpButton.setToolTipText("Show every command and what to type for it");
		helpButton.addActionListener(e -> showHelp.run());
		final JPanel footer = new JPanel(new BorderLayout(6, 0));
		footer.setOpaque(false);
		footer.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 0, 0, 0)));
		// wrapped to the room the button leaves it
		help = new SideTab.Wrapping("", SideTab.TEXT_WIDTH - 6 - helpButton.getPreferredSize().width);
		help.setFont(SideTab.small());
		help.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		footer.add(help, BorderLayout.CENTER);
		footer.add(helpButton, BorderLayout.EAST);
		updateHelp();

		add(group, BorderLayout.NORTH);
		add(display, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);
	}

	@Override
	public void onActivate()
	{
		// the hotkey may have been changed in the settings since
		updateHelp();
		contents.get(selected).refresh();
		ticker.start();
	}

	@Override
	public void onDeactivate()
	{
		ticker.stop();
	}

	/**
	 * Shows the tab. The panel itself is opened through the sidebar.
	 */
	void select(Tab tab)
	{
		group.select(tabs.get(tab));
	}

	Tab getSelected()
	{
		return selected;
	}

	/**
	 * Updates the tab, if it is the one on screen.
	 */
	void refreshIfShowing(Tab tab)
	{
		if (isShowing() && selected == tab)
		{
			contents.get(tab).refresh();
		}
	}

	JLabel getHelp()
	{
		return help;
	}

	private void updateHelp()
	{
		final String keys = hotkey.get();
		help.setText("<html>Need help? Open the palette" + (keys == null ? "" : " with <b>" + keys + "</b>")
			+ " and type <b>help</b>, or click Help.</html>");
	}

	void stop()
	{
		ticker.stop();
	}

	private void tick()
	{
		if (isShowing() && contents.get(selected).isLive())
		{
			contents.get(selected).refresh();
		}
	}
}
