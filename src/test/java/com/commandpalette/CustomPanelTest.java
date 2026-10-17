package com.commandpalette;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

public class CustomPanelTest
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

	private List<CustomCommand> saved = new ArrayList<>();
	private final List<Command> available = List.of(
		new Command("panel:Loot Tracker", "Loot Tracker", "Open panel", null, () -> {}),
		new Command("settings:GPU", "GPU", "Open settings", null, () -> {}),
		new Command("custom:old", "old", "", null, () -> {}));
	// a fresh copy each time, as the settings would give
	private final CustomPanel panel = new CustomPanel(() -> new ArrayList<>(copy(saved)),
		commands -> saved = new ArrayList<>(commands), () -> available);

	private static List<CustomCommand> copy(List<CustomCommand> commands)
	{
		final List<CustomCommand> copies = new ArrayList<>();
		commands.forEach(c -> copies.add(new CustomCommand(c.getName(), c.getAction(), c.getValue())));
		return copies;
	}

	@Test
	public void startersAreAddedWithoutTakingANameTwice()
	{
		saved = new ArrayList<>(List.of(new CustomCommand("WOM", CustomCommand.Action.COPY, "mine")));
		panel.refresh();
		panel.addStarters();
		final List<String> names = new ArrayList<>();
		saved.forEach(c -> names.add(c.getName()));
		assertEquals(List.of("WOM", "ge", "hiscores", "brag"), names);
	}

	@Test
	public void aVariableClickedGoesWhereTheCursorIs()
	{
		panel.openEditor(null);
		panel.getAction().setSelectedItem(CustomCommand.Action.SHOW);
		panel.getValueField().setText("Me:  total");
		panel.getValueField().setCaretPosition(4);
		panel.insert("{rsn}");
		assertEquals("Me: {rsn} total", panel.getValueField().getText());
	}

	@Test
	public void everyVariableListedIsOne()
	{
		final List<VariableCatalog.Variable> all = new ArrayList<>(VariableCatalog.OWN);
		all.addAll(VariableCatalog.GROUND_MARKER_VARIABLES);
		for (VariableCatalog.Variable variable : all)
		{
			assertTrue(variable.getToken(), VariableResolver.hasVariables(variable.getToken()));
		}
		// filled in from what is typed, not as a variable
		assertFalse(VariableResolver.hasVariables(VariableCatalog.TEXT.getToken()));
	}

	@Test
	public void aNewCommandIsSaved()
	{
		panel.refresh();
		panel.openEditor(null);
		panel.getNameField().setText("herbs");
		panel.getAction().setSelectedItem(CustomCommand.Action.TIMER);
		panel.getValueField().setText("80m herb run");
		panel.saveEditor();
		assertEquals(List.of(new CustomCommand("herbs", CustomCommand.Action.TIMER, "80m herb run")), saved);
		assertFalse(panel.isEditorOpen());
	}

	@Test
	public void whatIsWrongIsSaidAndNothingIsSaved()
	{
		panel.openEditor(null);
		panel.getNameField().setText("herbs");
		panel.getAction().setSelectedItem(CustomCommand.Action.TIMER);
		panel.getValueField().setText("herb run");
		panel.saveEditor();
		assertTrue(saved.isEmpty());
		assertTrue(panel.isEditorOpen());
		assertTrue(panel.getError().isVisible());
		assertTrue(panel.getError().getText().contains("Start with a length"));
	}

	@Test
	public void changingACommandKeepsItsPlaceAndItsName()
	{
		saved.add(new CustomCommand("ge", CustomCommand.Action.LINK, "https://example.com/?q={text}"));
		saved.add(new CustomCommand("herbs", CustomCommand.Action.TIMER, "80m herb run"));
		panel.openEditor(copy(saved).get(1));
		panel.getValueField().setText("90m herb run");
		panel.saveEditor();
		assertEquals(List.of(new CustomCommand("ge", CustomCommand.Action.LINK, "https://example.com/?q={text}"),
			new CustomCommand("herbs", CustomCommand.Action.TIMER, "90m herb run")), saved);
	}

	@Test
	public void aCommandToRunIsPickedFromTheOthersButNotTheCustomOnes()
	{
		panel.openEditor(null);
		panel.getAction().setSelectedItem(CustomCommand.Action.COMMAND);
		final List<String> offered = new ArrayList<>();
		for (int i = 0; i < panel.getCommand().getItemCount(); i++)
		{
			offered.add(panel.getCommand().getItemAt(i).getKey());
		}
		// sorted by title
		assertEquals(List.of("settings:GPU", "panel:Loot Tracker"), offered);

		panel.getNameField().setText("loot");
		panel.getCommand().setSelectedIndex(1);
		panel.saveEditor();
		assertEquals(List.of(new CustomCommand("loot", CustomCommand.Action.COMMAND, "panel:Loot Tracker")), saved);
	}
}
