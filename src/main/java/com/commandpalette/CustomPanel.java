package com.commandpalette;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * The side panel's Custom tab: the user's own commands, made from the palette's own actions, to add, change and
 * remove.
 */
class CustomPanel extends SideTab
{
	static final String VARIABLES_HINT = "Variables such as <b>{rsn}</b> fill in: click one under <b>Variables</b>"
		+ " below to put it in.";
	// how many examples a click on "Add some to start" adds
	static final int STARTERS = 4;

	// a link can't wrap, so the hint has none
	private static final String LINK_HINT = "The page to open, starting https://. Put <b>{text}</b> where a search"
		+ " goes, and what you type after the name fills it: <b>ge abyssal whip</b>. " + VARIABLES_HINT;
	private static final String TIMER_HINT = "A length, then what it is for: <b>80m herb run</b>.";
	private static final String COMMAND_HINT = "Any command the palette lists: a side panel, a plugin's settings, a"
		+ " world, a tab of this panel and the rest.";
	private static final String COPY_HINT = "The text to copy, such as a clan chat's name. " + VARIABLES_HINT;
	private static final String SHOW_HINT = "Shown in the chat, where only you see it, and kept in Answers: <b>{rsn}"
		+ " is {lvl_total} total</b>. " + VARIABLES_HINT;

	private final Supplier<List<CustomCommand>> load;
	private final Consumer<List<CustomCommand>> save;
	private final Supplier<List<Command>> available;

	private final JPanel content = new JPanel(new BorderLayout(0, 6));

	// the editor's fields, while it is open
	private final JTextField name = new JTextField();
	private final JComboBox<CustomCommand.Action> action = new JComboBox<>(CustomCommand.Action.values());
	// wraps and grows as it fills, as a link or a line of text can be long
	private final JTextArea value = new JTextArea()
	{
		@Override
		public Dimension getPreferredSize()
		{
			// before it is laid out it has no width to wrap to, so it takes the panel's
			if (getWidth() == 0)
			{
				setSize(TEXT_WIDTH, Short.MAX_VALUE);
			}
			return super.getPreferredSize();
		}
	};
	private final JComboBox<Command> command = new JComboBox<>();
	private final JLabel valueLabel = label("");
	private final JLabel hint = note("");
	private final JLabel error = note("");
	private final JPanel valueHolder = new JPanel(new BorderLayout());
	// the one being changed, or null for a new one
	private CustomCommand editing;
	private boolean editorOpen;
	private Boolean examplesOpen;
	// whether the variables are listed under the commands, and under the editor
	private boolean variablesOpen;
	private boolean editorVariablesOpen = true;
	// the variables under the editor, there only for an action that fills them in
	private final JPanel editorVariables = new JPanel(new BorderLayout());

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
		value.setLineWrap(true);
		value.setWrapStyleWord(true);
		value.setFont(name.getFont());
		value.setBorder(UIManager.getBorder("TextField.border"));
		value.setBackground(name.getBackground());
		// a command is one line: Enter saves, as in the name, and a pasted line break becomes a space
		value.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "save");
		value.getActionMap().put("save", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveEditor();
			}
		});
		// Tab moves on, as from a field, not putting one in
		value.getInputMap().put(KeyStroke.getKeyStroke("TAB"), "next");
		value.getActionMap().put("next", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				value.transferFocus();
			}
		});
		value.getInputMap().put(KeyStroke.getKeyStroke("shift TAB"), "previous");
		value.getActionMap().put("previous", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				value.transferFocusBackward();
			}
		});
		((AbstractDocument) value.getDocument()).setDocumentFilter(new DocumentFilter()
		{
			@Override
			public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
				throws BadLocationException
			{
				super.insertString(fb, offset, oneLine(text), attr);
			}

			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				throws BadLocationException
			{
				super.replace(fb, offset, length, oneLine(text), attrs);
			}
		});
		value.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				grow();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				grow();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				grow();
			}
		});
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

	private static String oneLine(String text)
	{
		return text == null ? null : text.replaceAll("\\R", " ");
	}

	// lays the editor out again, for the value's field to take as many lines as its text needs
	private void grow()
	{
		content.revalidate();
		content.repaint();
	}

	JTextArea getValueField()
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
			+ " another command, or text to copy or show, with your stats filled in. They are listed and searched like"
			+ " any other."), BorderLayout.SOUTH);

		final JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		list.setOpaque(false);
		final List<CustomCommand> commands = load.get();
		if (commands.isEmpty())
		{
			final JButton starters = new JButton("Add some to start");
			starters.setFont(small());
			starters.setToolTipText("Add a few of the examples below");
			starters.addActionListener(e -> addStarters());
			final JPanel empty = new JPanel(new BorderLayout(0, 6));
			empty.setOpaque(false);
			empty.add(note("None yet. Click Add to make one, or start from the examples."), BorderLayout.NORTH);
			empty.add(starters, BorderLayout.SOUTH);
			list.add(empty);
		}
		final List<Command> all = available.get();
		for (CustomCommand custom : commands)
		{
			list.add(row(custom, all));
		}

		// open to begin with while there are none
		final boolean open = examplesOpen != null ? examplesOpen : commands.isEmpty();
		final JButton toggle = new JButton(open ? "Hide" : "Show");
		toggle.setFont(small());
		toggle.addActionListener(e ->
		{
			examplesOpen = !open;
			showList();
		});
		list.add(section("Examples", toggle));
		if (open)
		{
			for (CustomCommand example : CustomCommands.EXAMPLES)
			{
				if (commands.stream().noneMatch(c -> c.getName().equalsIgnoreCase(example.getName())))
				{
					list.add(exampleRow(example));
				}
			}
		}

		list.add(section("Variables", variablesOpen, () ->
		{
			variablesOpen = !variablesOpen;
			showList();
		}));
		if (variablesOpen)
		{
			list.add(variables("Click one to copy it, to paste in a command.", "Copy", token ->
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(token), null)));
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

	private JPanel exampleRow(CustomCommand example)
	{
		final JPanel row = row();
		final JLabel title = new JLabel(example.getName());
		title.setForeground(ColorScheme.TEXT_COLOR);
		// a link is too long to wrap, so it is said in words
		final String what = example.getAction() == CustomCommand.Action.LINK && example.getValue().contains("{rsn}")
			? example.describe(key -> null) + " for your name" : example.describe(key -> null);
		final JLabel detail = new Wrapping("<html>" + escape(what) + "</html>", ROW_TEXT_WIDTH);
		detail.setFont(small());
		detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		final JPanel text = new JPanel(new BorderLayout());
		text.setOpaque(false);
		text.add(title, BorderLayout.NORTH);
		text.add(detail, BorderLayout.SOUTH);
		final JButton add = iconButton(Icons.CUSTOM, "Add " + example.getName());
		add.addActionListener(e -> addExamples(List.of(example)));
		final JPanel holder = new JPanel(new BorderLayout());
		holder.setOpaque(false);
		holder.add(add, BorderLayout.NORTH);
		row.add(text, BorderLayout.CENTER);
		row.add(holder, BorderLayout.EAST);
		return row;
	}

	void addStarters()
	{
		addExamples(CustomCommands.EXAMPLES.subList(0, STARTERS));
	}

	// those whose names aren't taken, up to the most there can be
	private void addExamples(List<CustomCommand> examples)
	{
		final List<CustomCommand> commands = new ArrayList<>(load.get());
		for (CustomCommand example : examples)
		{
			if (commands.size() < CustomCommands.MAX
				&& commands.stream().noneMatch(c -> c.getName().equalsIgnoreCase(example.getName())))
			{
				commands.add(example);
			}
		}
		save.accept(commands);
		showList();
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
		// after the buttons, which stay where they are whether it is open or not; a hidden list takes no room in a
		// border layout, where it would in the grid
		editorVariables.setOpaque(false);
		showEditorVariables();
		final JPanel variablesHolder = new JPanel(new BorderLayout());
		variablesHolder.setOpaque(false);
		variablesHolder.add(editorVariables, BorderLayout.NORTH);
		form.add(variablesHolder);

		// taller than the panel can be with the variables open
		content.add(scrolling(form), BorderLayout.CENTER);
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
			case SHOW:
				valueLabel.setText("Text");
				hint.setText("<html>" + SHOW_HINT + "</html>");
				break;
			default:
				valueLabel.setText("Text");
				hint.setText("<html>" + COPY_HINT + "</html>");
				break;
		}
		// a timer and a command have no text for variables to go in
		editorVariables.setVisible(chosen != CustomCommand.Action.TIMER && chosen != CustomCommand.Action.COMMAND);
		valueHolder.revalidate();
		valueHolder.repaint();
	}

	// the variables under the editor, each put in the field where the cursor is when clicked
	private void showEditorVariables()
	{
		editorVariables.removeAll();
		final JPanel inside = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		inside.setOpaque(false);
		inside.add(section("Variables", editorVariablesOpen, () ->
		{
			editorVariablesOpen = !editorVariablesOpen;
			showEditorVariables();
		}));
		if (editorVariablesOpen)
		{
			inside.add(variables("Click one to put it in where the cursor is.", "Put in", this::insert));
		}
		editorVariables.add(inside, BorderLayout.CENTER);
		editorVariables.revalidate();
		editorVariables.repaint();
	}

	void insert(String token)
	{
		value.replaceSelection(token);
		value.requestFocusInWindow();
	}

	/**
	 * Every variable there is to use, under what it does, each doing something when clicked.
	 *
	 * @param how     how to use them
	 * @param verb    what clicking one does, "Copy"
	 * @param clicked what to do with the variable clicked
	 */
	private JPanel variables(String how, String verb, Consumer<String> clicked)
	{
		final JPanel variables = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		variables.setOpaque(false);
		variables.add(note(how + " Each fills in when the command runs, with its bare value; <b>{&amp;...}</b> gives"
			+ " a labelled one where there is one. In a link, each is made safe to be in it."));
		variables.add(subheading("What you type"));
		variables.add(variableRow(VariableCatalog.TEXT, verb, clicked));
		variables.add(subheading("Always there"));
		for (VariableCatalog.Variable variable : VariableCatalog.OWN)
		{
			variables.add(variableRow(variable, verb, clicked));
		}
		variables.add(subheading("With Ground Marker Variables"));
		variables.add(note("These need that plugin installed. A few of its many: its page on the Plugin Hub lists"
			+ " them all."));
		for (VariableCatalog.Variable variable : VariableCatalog.GROUND_MARKER_VARIABLES)
		{
			variables.add(variableRow(variable, verb, clicked));
		}
		return variables;
	}

	private static JPanel variableRow(VariableCatalog.Variable variable, String verb, Consumer<String> clicked)
	{
		final JPanel row = row();
		final JLabel token = new Wrapping("<html>" + escape(variable.getToken()) + "</html>", TEXT_WIDTH - 2 * 8);
		token.setForeground(ColorScheme.TEXT_COLOR);
		final JLabel description = new Wrapping("<html>" + escape(variable.getDescription()) + "</html>",
			TEXT_WIDTH - 2 * 8);
		description.setFont(small());
		description.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(token, BorderLayout.NORTH);
		row.add(description, BorderLayout.SOUTH);
		row.setToolTipText(verb + " " + variable.getToken());
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				clicked.accept(variable.getToken());
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
		return row;
	}

	// a heading with a button that opens or closes what is under it
	private static JPanel section(String title, boolean open, Runnable toggle)
	{
		final JButton button = new JButton(open ? "Hide" : "Show");
		button.setFont(small());
		button.addActionListener(e -> toggle.run());
		return section(title, button);
	}

	private static JPanel section(String title, JButton button)
	{
		final JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);
		header.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		header.add(heading(title), BorderLayout.CENTER);
		header.add(button, BorderLayout.EAST);
		return header;
	}

	private static JLabel subheading(String text)
	{
		final JLabel label = new JLabel(text);
		label.setFont(small());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		return label;
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
