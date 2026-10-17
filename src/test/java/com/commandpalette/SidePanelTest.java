package com.commandpalette;

import java.awt.Component;
import java.awt.Container;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.client.ui.PluginPanel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

/**
 * The side panel's tabs other than the Worlds one, which has a test of its own.
 */
public class SidePanelTest
{
	// Swing components are only to be used on the event thread, as they are in the client
	@Rule
	public final TestRule onEventThread = (base, description) -> new Statement()
	{
		@Override
		public void evaluate() throws Throwable
		{
			final Throwable[] thrown = new Throwable[1];
			SwingUtilities.invokeAndWait(() ->
			{
				try
				{
					base.evaluate();
				}
				catch (Throwable t)
				{
					thrown[0] = t;
				}
			});
			if (thrown[0] != null)
			{
				throw thrown[0];
			}
		}
	};

	// every label's text within the component, in order
	private static List<String> texts(Container container)
	{
		final List<String> texts = new ArrayList<>();
		for (Component component : container.getComponents())
		{
			if (component instanceof JLabel)
			{
				texts.add(((JLabel) component).getText());
			}
			else if (component instanceof Container)
			{
				texts.addAll(texts((Container) component));
			}
		}
		return texts;
	}

	private static JButton button(Container container, String tooltip)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JButton && tooltip.equals(((JButton) component).getToolTipText()))
			{
				return (JButton) component;
			}
			if (component instanceof Container)
			{
				final JButton found = button((Container) component, tooltip);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static final class Counting extends SideTab
	{
		private int refreshes;

		@Override
		void refresh()
		{
			refreshes++;
		}
	}

	@Test
	public void selectingATabRefreshesIt()
	{
		final Map<SidePanel.Tab, SideTab> contents = new EnumMap<>(SidePanel.Tab.class);
		final Map<SidePanel.Tab, Counting> counting = new EnumMap<>(SidePanel.Tab.class);
		for (SidePanel.Tab tab : SidePanel.Tab.values())
		{
			counting.put(tab, new Counting());
			contents.put(tab, counting.get(tab));
		}
		final SidePanel panel = new SidePanel(contents, () -> "Ctrl+P", () -> {});
		assertEquals(SidePanel.Tab.WORLDS, panel.getSelected());
		assertEquals(1, counting.get(SidePanel.Tab.WORLDS).refreshes);

		panel.select(SidePanel.Tab.TIMERS);
		assertEquals(SidePanel.Tab.TIMERS, panel.getSelected());
		assertEquals(1, counting.get(SidePanel.Tab.TIMERS).refreshes);
		panel.stop();
	}

	@Test
	public void theFooterSaysHowToGetHelpWithTheHotkeyAsItIsNow()
	{
		final String[] hotkey = {"Ctrl+P"};
		final int[] helps = new int[1];
		final Map<SidePanel.Tab, SideTab> contents = new EnumMap<>(SidePanel.Tab.class);
		for (SidePanel.Tab tab : SidePanel.Tab.values())
		{
			contents.put(tab, new Counting());
		}
		final SidePanel panel = new SidePanel(contents, () -> hotkey[0], () -> helps[0]++);
		assertTrue(panel.getHelp().getText().contains("<b>Ctrl+P</b> and type <b>help</b>"));

		hotkey[0] = "F1";
		panel.onActivate();
		assertTrue(panel.getHelp().getText().contains("<b>F1</b>"));

		hotkey[0] = null;
		panel.onActivate();
		assertTrue(panel.getHelp().getText().contains("Open the palette and type <b>help</b>"));

		button(panel, "Show every command and what to type for it").doClick(0);
		assertEquals(1, helps[0]);
		panel.stop();
	}

	@Test
	public void theTimersTabStartsWhatIsTypedAndListsTheRunningOnes()
	{
		final List<Timers.Request> started = new ArrayList<>();
		final TimersPanel panel = new TimersPanel(List::of, started::add);
		panel.refresh();
		assertTrue(texts(panel).contains("<html>No timers are running.</html>"));

		panel.getField().setText("5m herb run");
		panel.getField().postActionEvent();
		assertEquals("5m timer: herb run", started.get(0).describe());
		assertEquals("", panel.getField().getText());

		panel.getField().setText("herb run");
		panel.getField().postActionEvent();
		assertEquals(1, started.size());
		assertTrue(panel.getError().isVisible());

		// a length's button uses what is typed as what the timer is for
		button(panel, "Start a 10m timer, for what is typed above if anything").doClick(0);
		assertEquals("10m timer: herb run", started.get(1).describe());
		assertFalse(panel.getError().isVisible());
	}

	@Test
	public void theSessionTabShowsTheXpGained()
	{
		final SessionPanel panel = new SessionPanel(then -> then.accept(null), skill -> null);
		panel.refresh();
		assertTrue(texts(panel.getContent()).get(1).startsWith("<html>Log in"));

		final int[] start = new int[Skill.values().length];
		final int[] now = start.clone();
		now[Skill.SLAYER.ordinal()] = 30_000;
		now[Skill.ATTACK.ordinal()] = 10_000;
		panel.show(new SessionCommands.Snapshot(Duration.ofMinutes(30), SessionCommands.gains(start, now)));
		assertEquals(List.of("This session", "Logged in", "30m", "Xp gained", "40,000", "Xp an hour", "80,000",
			"By skill", "Slayer", "30,000", "Attack", "10,000"), texts(panel.getContent()));
	}

	@Test
	public void theAnswersTabListsTheAnswersLatestFirst()
	{
		final Answers answers = new Answers();
		final AnswersPanel panel = new AnswersPanel(answers);
		answers.add("tax 12m: 240k", null);
		answers.add("1 < 2 & 3", null);
		panel.refresh();
		assertEquals(List.of("<html>1 &lt; 2 &amp; 3</html>", "<html>tax 12m: 240k</html>"),
			texts(panel.getList()));
	}

	// lays the tab out in a side panel as wide as the client's, and says how far its widest button reaches
	private static void assertButtonsFit(SideTab tab)
	{
		final Map<SidePanel.Tab, SideTab> contents = new EnumMap<>(SidePanel.Tab.class);
		for (SidePanel.Tab each : SidePanel.Tab.values())
		{
			contents.put(each, each == SidePanel.Tab.WORLDS ? tab : new Counting());
		}
		final SidePanel panel = new SidePanel(contents, () -> "Ctrl+P", () -> {});
		panel.setSize(panel.getPreferredSize().width, 600);
		layOut(panel);
		for (JButton button : buttons(tab))
		{
			final java.awt.Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(),
				button.getBounds(), panel);
			assertTrue(button.getToolTipText() + " reaches " + bounds.getMaxX() + " of " + panel.getWidth(),
				bounds.getMaxX() <= panel.getWidth() - PluginPanel.BORDER_OFFSET);
		}
		panel.stop();
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

	private static List<JButton> buttons(Container container)
	{
		final List<JButton> buttons = new ArrayList<>();
		for (Component component : container.getComponents())
		{
			if (component instanceof JButton)
			{
				buttons.add((JButton) component);
			}
			else if (component instanceof Container)
			{
				buttons.addAll(buttons((Container) component));
			}
		}
		return buttons;
	}

	@Test
	public void thePinsFitWhenNothingIsPinned()
	{
		final List<Command> commands = List.of(
			new Command("panel:Loot Tracker", "Loot Tracker", "Open panel", null, () -> {}));
		final PinnedPanel panel = new PinnedPanel(() -> commands, command -> {}, List::of, command -> {},
			() -> List.of("panel:Loot Tracker"));
		panel.refresh();
		assertButtonsFit(panel);
	}

	@Test
	public void theCopyButtonsFitBesideALongAnswer()
	{
		final Answers answers = new Answers();
		answers.add("Rune platebody: GE 38,512 gp, high alch 39,000 gp, which is a long answer to fit", null);
		final AnswersPanel panel = new AnswersPanel(answers);
		panel.refresh();
		assertButtonsFit(panel);
	}

	@Test
	public void thePinnedTabListsPinsThenRecentOnes()
	{
		final List<Command> commands = List.of(
			new Command("panel:Loot Tracker", "Loot Tracker", "Open panel", null, () -> {}),
			new Command("world:302", "World 302", "Members", null, () -> {}),
			new Command("help", "Help", "", null, () -> {}));
		final List<String> pinned = new ArrayList<>(List.of("help"));
		final List<String> ran = new ArrayList<>();
		final PinnedPanel panel = new PinnedPanel(() -> commands, command -> ran.add(command.getKey()),
			() -> pinned, command -> pinned.add(command.getKey()), () -> List.of("world:302", "help"));
		panel.refresh();
		assertEquals(List.of("Pinned", "Help", "Recently used", "World 302", "Members"), texts(panel.getList()));

		button(panel, "Pin").doClick(0);
		assertEquals(List.of("help", "world:302"), pinned);
		assertEquals(List.of("Pinned", "Help", "World 302", "Members", "Recently used",
			"<html>The commands you run from the palette show up here, to run again or pin.</html>"),
			texts(panel.getList()));
		assertTrue(ran.isEmpty());
	}
}
