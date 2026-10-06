package com.commandpalette;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Against a stand-in for the client window laid out the way ClientUI builds it.
 */
public class SidebarCommandsTest
{
	private int forcedOpens;
	private final SidebarCommands source = new SidebarCommands(() -> forcedOpens++);

	private JPanel window;
	private JPanel titleBar;
	private JTabbedPane sidebar;
	private final List<String> activations = new ArrayList<>();

	@Before
	public void buildWindow()
	{
		sidebar = new JTabbedPane(JTabbedPane.RIGHT);
		// a panel with tabs of its own, which must not be mistaken for the sidebar
		final JPanel config = new JPanel();
		config.add(new JTabbedPane(JTabbedPane.RIGHT));
		sidebar.insertTab(null, new ImageIcon(), config, "Configuration", 0);
		sidebar.insertTab(null, new ImageIcon(), new JPanel(), "Loot Tracker", 1);
		sidebar.insertTab(null, new ImageIcon(), new JPanel(), "", 2);
		sidebar.setSelectedIndex(-1);
		// the client activates a panel when it is selected on a visible sidebar
		sidebar.addChangeListener(e ->
		{
			if (sidebar.isVisible() && sidebar.getSelectedIndex() >= 0)
			{
				activations.add(sidebar.getToolTipTextAt(sidebar.getSelectedIndex()));
			}
		});

		final JPanel content = new JPanel();
		content.add(new JPanel());
		content.add(sidebar);
		titleBar = new JPanel();
		window = new JPanel();
		window.add(titleBar);
		window.add(content);
	}

	private List<Command> commands()
	{
		final List<Command> commands = new ArrayList<>();
		source.collect(window, commands);
		return commands;
	}

	private List<String> describe()
	{
		return commands().stream().map(c -> c.getTitle() + " / " + c.getDetail()).collect(Collectors.toList());
	}

	// the client's own button, which shows the sidebar and activates the selected panel
	private JButton addOpenSidebarButton()
	{
		final JButton button = new JButton();
		button.setToolTipText("Open sidebar");
		button.addActionListener(e ->
		{
			sidebar.setVisible(true);
			activations.add(sidebar.getToolTipTextAt(sidebar.getSelectedIndex()));
		});
		titleBar.add(button);
		return button;
	}

	@Test
	public void listsEveryPanelInSidebarOrder()
	{
		assertEquals(List.of("Configuration / Open panel", "Loot Tracker / Open panel", "Panel 3 / Open panel"), describe());
		assertSame(sidebar.getIconAt(1), commands().get(1).getIcon());
	}

	@Test
	public void marksTheShowingPanelAndOffersToCloseIt()
	{
		sidebar.setSelectedIndex(1);
		assertEquals(List.of("Configuration / Open panel", "Loot Tracker / Open panel (showing)", "Panel 3 / Open panel",
			"Close side panel / "), describe());

		commands().get(3).run();
		assertEquals(-1, sidebar.getSelectedIndex());
	}

	@Test
	public void aSelectedPanelOnAHiddenSidebarIsNotShowing()
	{
		sidebar.setSelectedIndex(1);
		sidebar.setVisible(false);
		assertEquals(List.of("Configuration / Open panel", "Loot Tracker / Open panel", "Panel 3 / Open panel"), describe());
	}

	@Test
	public void opensAPanelOnAVisibleSidebar()
	{
		commands().get(1).run();
		assertEquals(1, sidebar.getSelectedIndex());
		assertEquals(List.of("Loot Tracker"), activations);
	}

	@Test
	public void followsAPanelThatMovedSinceTheCommandsWereListed()
	{
		final Command loot = commands().get(1);
		sidebar.removeTabAt(0);
		loot.run();
		assertEquals("Loot Tracker", sidebar.getToolTipTextAt(sidebar.getSelectedIndex()));
	}

	@Test
	public void doesNothingForAPanelThatIsGone()
	{
		final Command loot = commands().get(1);
		sidebar.removeTabAt(1);
		loot.run();
		assertEquals(-1, sidebar.getSelectedIndex());
	}

	@Test
	public void reopensAHiddenSidebarWithTheClientsButton()
	{
		addOpenSidebarButton();
		sidebar.setVisible(false);

		commands().get(1).run();

		assertTrue(sidebar.isVisible());
		assertEquals(1, sidebar.getSelectedIndex());
		assertEquals(List.of("Loot Tracker"), activations);
		assertEquals(0, forcedOpens);
	}

	@Test
	public void offersToShowAHiddenSidebarOnlyWithTheClientsButton()
	{
		sidebar.setVisible(false);
		assertEquals(3, commands().size());

		addOpenSidebarButton();
		sidebar.setSelectedIndex(0);
		final Command show = commands().get(3);
		assertEquals("Show sidebar", show.getTitle());
		show.run();
		assertTrue(sidebar.isVisible());
	}

	@Test
	public void reopensAHiddenSidebarWithoutAButton()
	{
		// already selected when the sidebar was hidden, so selecting it again would not activate it
		sidebar.setSelectedIndex(1);
		sidebar.setVisible(false);
		activations.clear();

		commands().get(1).run();

		assertTrue(sidebar.isVisible());
		assertEquals(1, sidebar.getSelectedIndex());
		assertEquals(List.of("Loot Tracker"), activations);
		assertEquals(1, forcedOpens);
	}
}
