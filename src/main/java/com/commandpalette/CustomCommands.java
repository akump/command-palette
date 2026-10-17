package com.commandpalette;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.swing.Icon;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.LinkBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The commands the user has made in the side panel's Custom tab, kept in this plugin's settings of each profile.
 */
class CustomCommands
{
	private static final Logger log = LoggerFactory.getLogger(CustomCommands.class);

	static final String KIND = "custom:";
	static final int MAX = 100;

	// how often, in game ticks, what the commands with variables would show or copy is read again
	static final int PREVIEW_TICKS = 5;

	/**
	 * Commands to start from, most of them with the player's name filled in.
	 */
	static final List<CustomCommand> EXAMPLES = List.of(
		new CustomCommand("ge", CustomCommand.Action.LINK, "https://prices.runescape.wiki/osrs/all-items?search={text}"),
		new CustomCommand("wom", CustomCommand.Action.LINK, "https://wiseoldman.net/players/{rsn}"),
		new CustomCommand("hiscores", CustomCommand.Action.LINK,
			"https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal?user1={rsn}"),
		new CustomCommand("brag", CustomCommand.Action.SHOW, "{rsn}: total {lvl_total}, combat {lvl_combat}"),
		new CustomCommand("temple", CustomCommand.Action.LINK, "https://templeosrs.com/player/overview.php?player={rsn}"),
		new CustomCommand("clog", CustomCommand.Action.LINK,
			"https://templeosrs.com/player/collection-log.php?player={rsn}"),
		new CustomCommand("rsn", CustomCommand.Action.COPY, "{rsn}"));

	private static final String CONFIG_KEY = "customCommands";

	private final ConfigManager configManager;
	private final Gson gson;
	private final TypedCommands typedCommands;
	private final VariableResolver variables;
	// what each command that shows or copies text with variables in it would, by name, read every few ticks
	private volatile Map<String, String> previews = Collections.emptyMap();

	@Inject
	CustomCommands(ConfigManager configManager, Gson gson, TypedCommands typedCommands, VariableResolver variables)
	{
		this.configManager = configManager;
		this.gson = gson;
		this.typedCommands = typedCommands;
		this.variables = variables;
	}

	List<CustomCommand> load()
	{
		return parse(gson, configManager.getConfiguration(CommandPaletteConfig.GROUP, CONFIG_KEY));
	}

	void save(List<CustomCommand> commands)
	{
		if (commands.isEmpty())
		{
			configManager.unsetConfiguration(CommandPaletteConfig.GROUP, CONFIG_KEY);
		}
		else
		{
			configManager.setConfiguration(CommandPaletteConfig.GROUP, CONFIG_KEY, toJson(gson, commands));
		}
	}

	/**
	 * The saved commands, leaving out any that can't be read.
	 */
	static List<CustomCommand> parse(Gson gson, String json)
	{
		final List<CustomCommand> commands = new ArrayList<>();
		if (json == null || json.isEmpty())
		{
			return commands;
		}
		try
		{
			for (JsonElement element : gson.fromJson(json, JsonArray.class))
			{
				final JsonObject saved = element.getAsJsonObject();
				final CustomCommand.Action action = CustomCommand.Action.valueOf(saved.get("action").getAsString());
				commands.add(new CustomCommand(saved.get("name").getAsString(), action, saved.get("value").getAsString()));
			}
		}
		catch (RuntimeException e)
		{
			log.warn("Couldn't read the custom commands", e);
		}
		return commands;
	}

	static String toJson(Gson gson, List<CustomCommand> commands)
	{
		final JsonArray saved = new JsonArray();
		for (CustomCommand command : commands)
		{
			final JsonObject object = new JsonObject();
			object.addProperty("name", command.getName());
			object.addProperty("action", command.getAction().name());
			object.addProperty("value", command.getValue());
			saved.add(object);
		}
		return gson.toJson(saved);
	}

	/**
	 * A palette command for each custom one.
	 *
	 * @param find the palette command with that key, or null if there is none now
	 */
	void collect(List<Command> out, Function<String, Command> find)
	{
		for (CustomCommand custom : load())
		{
			out.add(new Command(custom.getKey(), custom.getName(), describe(custom, find), icon(custom, find),
				() -> run(custom, "", find)));
		}
	}

	/**
	 * For one that takes text, a command to run it with whatever is typed after its name: "ge abyssal whip".
	 */
	List<Command> forQuery(String typed, Function<String, Command> find)
	{
		final String text = typed.trim();
		final List<Command> commands = new ArrayList<>();
		for (CustomCommand custom : load())
		{
			final String prefix = custom.getName().toLowerCase(Locale.ROOT) + " ";
			if (custom.takesText() && text.toLowerCase(Locale.ROOT).startsWith(prefix)
				&& text.length() > prefix.length())
			{
				final String rest = text.substring(prefix.length()).trim();
				commands.add(new Command(custom.getName() + ": " + rest, describe(custom, find), Icons.LINK,
					() -> run(custom, rest, find)));
			}
		}
		return commands;
	}

	/**
	 * Reads again what each command that shows or copies text with variables in it would, on the client thread.
	 *
	 * @param resolve fills in the variables of a text
	 */
	void refreshPreviews(Function<String, String> resolve)
	{
		final Map<String, String> next = new HashMap<>();
		for (CustomCommand custom : load())
		{
			if (isPreviewed(custom))
			{
				next.put(custom.getName(), resolve.apply(custom.getValue()));
			}
		}
		previews = next;
	}

	/**
	 * Forgets the previews, which are of a player who has logged out.
	 */
	void clearPreviews()
	{
		previews = Collections.emptyMap();
	}

	private static boolean isPreviewed(CustomCommand custom)
	{
		return (custom.getAction() == CustomCommand.Action.SHOW || custom.getAction() == CustomCommand.Action.COPY)
			&& VariableResolver.hasVariables(custom.getValue());
	}

	// what it shows or copies with its variables filled in, when they have been read
	private String describe(CustomCommand custom, Function<String, Command> find)
	{
		final String preview = isPreviewed(custom) ? previews.get(custom.getName()) : null;
		if (preview != null && !preview.equals(custom.getValue()))
		{
			return (custom.getAction() == CustomCommand.Action.SHOW ? "Show " : "Copy ") + preview;
		}
		return custom.describe(key ->
		{
			final Command target = find.apply(key);
			return target == null ? null : target.getTitle();
		});
	}

	private static Icon icon(CustomCommand custom, Function<String, Command> find)
	{
		switch (custom.getAction())
		{
			case LINK:
				return Icons.LINK;
			case TIMER:
				return Icons.TIMER;
			case COMMAND:
				final Command target = find.apply(custom.getValue());
				return target == null || target.getIcon() == null ? Icons.PROMPT : target.getIcon();
			case SHOW:
				return Icons.CHAT;
			default:
				return Icons.CLIPBOARD;
		}
	}

	private void run(CustomCommand custom, String text, Function<String, Command> find)
	{
		switch (custom.getAction())
		{
			case LINK:
				// each variable's value is made safe for a link, and then what was typed is
				fill(custom.getValue(), true, url -> LinkBrowser.browse(link(url, text)));
				break;
			case TIMER:
				final Timers.Request timer = Timers.parse(custom.getValue());
				if (timer != null)
				{
					typedCommands.startTimer(timer);
				}
				break;
			case COMMAND:
				final Command target = find.apply(custom.getValue());
				if (target != null)
				{
					target.run();
				}
				break;
			case SHOW:
				fill(withText(custom.getValue(), text), false, shown -> typedCommands.show(shown, Icons.CHAT));
				break;
			default:
				fill(withText(custom.getValue(), text), false, copied ->
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(copied), null));
				break;
		}
	}

	// the variables are read on the client thread, so only text that has some waits for it
	private void fill(String text, boolean encode, Consumer<String> then)
	{
		if (VariableResolver.hasVariables(text))
		{
			variables.resolveLater(text, encode, then);
		}
		else
		{
			then.accept(text);
		}
	}

	static String withText(String value, String text)
	{
		return value.replace(CustomCommand.TEXT, text);
	}

	/**
	 * The link with the text in place of {text}, made safe to put in a link.
	 */
	static String link(String url, String text)
	{
		return url.replace(CustomCommand.TEXT, URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20"));
	}

	/**
	 * Whether a palette command can be run by a custom one: not another custom one, which could go round in a loop.
	 */
	static boolean canRun(Command command)
	{
		return command.getKey() != null && !command.getKey().startsWith(KIND);
	}

	static List<CustomCommand> without(List<CustomCommand> commands, CustomCommand removed)
	{
		final List<CustomCommand> rest = new ArrayList<>(commands);
		rest.remove(removed);
		return Collections.unmodifiableList(rest);
	}
}
