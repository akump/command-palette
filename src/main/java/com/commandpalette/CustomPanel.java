package com.commandpalette;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * The side panel's Custom tab: the user's own commands, made from the palette's own actions, to add, change and
 * remove.
 */
class CustomPanel extends SideTab
{
	// a link can't wrap, so the hint has none
	private static final String LINK_HINT = "The page to open, starting https://. Put <b>{text}</b> where a search"
		+ " goes, and what you type after the name fills it: <b>ge abyssal whip</b>.";
	private static final String TIMER_HINT = "A length, then what it is for: <b>80m herb run</b>.";
	private static final String COMMAND_HINT = "Any command the palette lists: a side panel, a plugin's settings, a"
		+ " world, a tab of this panel and the rest.";
	private static final String COPY_HINT = "The text to copy, such as a clan chat's name.";

	private final Supplier<List<CustomCommand>> load;
	private final Consumer<List<CustomCommand>> save;
	private final Supplier<List<Command>> available;

	private final JPanel content = new JPanel(new BorderLayout(0, 6));

	// the editor's fields, while it is open
	private final JTextField name = new JTextField();
	private final JComboBox<CustomCommand.Action> action = new JComboBox<>(CustomCommand.Action.values());
	private final JTextField value = new JTextField();
	private final JComboBox<Command> command = new JComboBox<>();
	private final JLabel valueLabel = label("");
	private final JLabel hint = note("");
	private final JLabel error = note("");
	private final JPanel valueHolder = new JPanel(new BorderLayout());
	// the one being changed, or null for a new one
	private CustomCommand editing;
	private boolean editorOpen;

	/**
	 * @param load      the custom commands as saved
	 * @param save      saves them
	 * @param available every palette command there is now, of which a custom one can run any but the custom ones
	 */
	CustomPanel(Supplier<List<CustomCommand>> load, Consumer<List<CustomCommand>> save,
		Supplier<List<Command>> available)
	{
		this.load = load;
		this.save = save;
		this.available = available;

		content.setOpaque(false);
		add(content, BorderLayout.CENTER);

		action.setFont(small());
		action.addActionListener(e -> showValue());
		command.setFont(small());
		command.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object item, int index, boolean selected,
				boolean focused)
			{
				final Command c = (Command) item;
				final String text = c == null ? "" : c.getDetail().isEmpty() ? c.getTitle()
					: c.getTitle() + " - " + c.getDetail();
				return super.getListCellRendererComponent(list, text, index, selected, focused);
			}
		});
		valueHolder.setOpaque(false);
		error.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		name.addActionListener(e -> saveEditor());
		value.addActionListener(e -> saveEditor());
	}

	@Override
	void refresh()
	{
		// the editor keeps what is typed into it, whatever happens meanwhile
		if (!editorOpen)
		{
			showList();
		}
	}

	JTextField getNameField()
	{
		return name;
	}

	JComboBox<CustomCommand.Action> getAction()
	{
		return action;
	}

	JTextField getValueField()
	{
		return value;
	}

	JComboBox<Command> getCommand()
	{
		return command;
	}

	JLabel getError()
	{
		return error;
	}

	JPanel getContent()
	{
		return content;
	}

	boolean isEditorOpen()
	{
		return editorOpen;
	}

	void showList()
	{
		editorOpen = false;
		content.removeAll();

		final JButton add = new JButton("Add");
		add.setFont(small());
		add.setToolTipText("Make a new command");
		add.addActionListener(e -> openEditor(null));
		final JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);
		header.add(heading("Custom commands"), BorderLayout.CENTER);
		header.add(add, BorderLayout.EAST);

		final JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setOpaque(false);
		top.add(header, BorderLayout.NORTH);
		top.add(note("Your own commands for the palette: a site to search, a timer you start often, a shortcut to"
			+ " another command, or text to copy. They are listed and searched like any other."), BorderLayout.SOUTH);

		final JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		list.setOpaque(false);
		final List<CustomCommand> commands = load.get();
		if (commands.isEmpty())
		{
			list.add(note("None yet. Click Add to make one."));
		}
		final List<Command> all = available.get();
		for (CustomCommand custom : commands)
		{
			list.add(row(custom, all));
		}

		content.add(top, BorderLayout.NORTH);
		content.add(scrolling(list), BorderLayout.CENTER);
		content.revalidate();
		content.repaint();
	}

	private JPanel row(CustomCommand custom, List<Command> all)
	{
		final JPanel row = row();
		final JLabel title = new JLabel(custom.getName());
		title.setForeground(ColorScheme.TEXT_COLOR);
		final JLabel detail = new JLabel(custom.describe(key -> all.stream()
			.filter(c -> key.equals(c.getKey())).map(Command::getTitle).findFirst().orElse(null)));
		detail.setFont(small());
		detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		final JPanel text = new JPanel(new BorderLayout());
		text.setOpaque(false);
		text.add(title, BorderLayout.NORTH);
		text.add(detail, BorderLayout.SOUTH);

		final JButton edit = iconButton(Icons.COG, "Change " + custom.getName());
		edit.addActionListener(e -> openEditor(custom));
		final JButton delete = iconButton(Icons.CLOSE, "Delete " + custom.getName());
		delete.addActionListener(e ->
		{
			if (JOptionPane.showConfirmDialog(this, "Delete the command " + custom.getName() + "?", "Command Palette",
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
			{
				save.accept(CustomCommands.without(load.get(), custom));
				showList();
			}
		});
		final JPanel buttons = new JPanel(new GridLayout(1, 2, 2, 0));
		buttons.setOpaque(false);
		buttons.add(edit);
		buttons.add(delete);

		row.add(text, BorderLayout.CENTER);
		row.add(buttons, BorderLayout.EAST);
		return row;
	}

	/**
	 * @param custom the command to change, or null for a new one
	 */
	void openEditor(CustomCommand custom)
	{
		editorOpen = true;
		editing = custom;
		content.removeAll();

		final List<Command> runnable = available.get().stream().filter(CustomCommands::canRun)
			.sorted(Comparator.comparing(Command::getTitle, String.CASE_INSENSITIVE_ORDER))
			.collect(Collectors.toList());
		command.removeAllItems();
		runnable.forEach(command::addItem);

		name.setText(custom == null ? "" : custom.getName());
		action.setSelectedItem(custom == null ? CustomCommand.Action.LINK : custom.getAction());
		value.setText(custom == null || custom.getAction() == CustomCommand.Action.COMMAND ? "" : custom.getValue());
		if (custom != null && custom.getAction() == CustomCommand.Action.COMMAND)
		{
			runnable.stream().filter(c -> custom.getValue().equals(c.getKey())).findFirst()
				.ifPresent(command::setSelectedItem);
		}
		error.setVisible(false);
		showValue();

		final JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> saveEditor());
		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> showList());
		final JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setOpaque(false);
		buttons.add(saveButton);
		buttons.add(cancel);

		final JPanel form = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		form.setOpaque(false);
		form.add(heading(custom == null ? "New command" : "Change " + custom.getName()));
		form.add(label("Name, as you type it in the palette"));
		form.add(name);
		form.add(label("What it does"));
		form.add(action);
		form.add(valueLabel);
		form.add(valueHolder);
		form.add(hint);
		// a hidden error takes no room in a border layout, where it would in the grid
		final JPanel bottom = new JPanel(new BorderLayout(0, 6));
		bottom.setOpaque(false);
		bottom.add(error, BorderLayout.NORTH);
		bottom.add(buttons, BorderLayout.SOUTH);
		form.add(bottom);

		content.add(form, BorderLayout.NORTH);
		content.revalidate();
		content.repaint();
		name.requestFocusInWindow();
	}

	// the field for what the chosen action needs, with a word on what to put in it
	private void showValue()
	{
		final CustomCommand.Action chosen = (CustomCommand.Action) action.getSelectedItem();
		valueHolder.removeAll();
		valueHolder.add(chosen == CustomCommand.Action.COMMAND ? command : value, BorderLayout.CENTER);
		switch (chosen)
		{
			case LINK:
				valueLabel.setText("Link");
				hint.setText("<html>" + LINK_HINT + "</html>");
				break;
			case TIMER:
				valueLabel.setText("Timer");
				hint.setText("<html>" + TIMER_HINT + "</html>");
				break;
			case COMMAND:
				valueLabel.setText("Command to run");
				hint.setText("<html>" + COMMAND_HINT + "</html>");
				break;
			default:
				valueLabel.setText("Text");
				hint.setText("<html>" + COPY_HINT + "</html>");
				break;
		}
		valueHolder.revalidate();
		valueHolder.repaint();
	}

	void saveEditor()
	{
		final CustomCommand.Action chosen = (CustomCommand.Action) action.getSelectedItem();
		final Command target = (Command) command.getSelectedItem();
		final String chosenValue = chosen == CustomCommand.Action.COMMAND
			? (target == null ? "" : target.getKey()) : value.getText().trim();

		final List<CustomCommand> commands = new ArrayList<>(load.get());
		final List<String> others = commands.stream().filter(c -> !c.equals(editing))
			.map(CustomCommand::getName).collect(Collectors.toList());
		if (editing == null && commands.size() >= CustomCommands.MAX)
		{
			showError("There can be up to " + CustomCommands.MAX + " custom commands.");
			return;
		}
		final String problem = CustomCommand.problem(name.getText(), chosen, chosenValue, others,
			key -> key != null && !key.isEmpty() && available.get().stream().anyMatch(c -> key.equals(c.getKey())));
		if (problem != null)
		{
			showError(problem);
			return;
		}

		final CustomCommand saved = new CustomCommand(name.getText().trim(), chosen, chosenValue);
		final int index = commands.indexOf(editing);
		if (index >= 0)
		{
			commands.set(index, saved);
		}
		else
		{
			commands.add(saved);
		}
		save.accept(commands);
		showList();
	}

	private void showError(String message)
	{
		error.setText("<html>" + escape(message) + "</html>");
		error.setVisible(true);
		content.revalidate();
	}

	private static JLabel label(String text)
	{
		final JLabel label = new JLabel(text);
		label.setFont(small());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JButton iconButton(Icon icon, String tooltip)
	{
		final JButton button = new JButton(icon);
		button.setToolTipText(tooltip);
		button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		button.setContentAreaFilled(false);
		return button;
	}
}
