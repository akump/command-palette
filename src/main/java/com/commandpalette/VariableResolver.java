package com.commandpalette;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

/**
 * Fills in the variables in a piece of text, "{rsn}" or "{lvl_agility}": a few of the palette's own, and every
 * other one that the Ground Marker Variables plugin knows, when it is installed, which it is asked for through
 * RuneLite's plugin messages. Only reads what the client already shows; nothing is sent to the game.
 */
@Singleton
class VariableResolver
{
	static final String NAMESPACE = "command-palette";
	static final String GMV_NAMESPACE = "ground-marker-variables";
	static final String RESOLVE = "Resolve";

	private static final Pattern NATIVE = Pattern.compile(
		"(rsn|world|time|time24|date|weekday|(lvl|boost|xp)_([a-z]+))", Pattern.CASE_INSENSITIVE);
	/**
	 * How a label styles its tile, which means nothing off it: {col=red}, {/b}, {highlightSW=yellow}, {align=left}.
	 */
	static final Pattern TILE_STYLE = Pattern.compile(
		"\\{/?(?:col|tileCol|highlight[a-z]*|align|b|i|borderWidth)(?:=[^{}]*)?\\}", Pattern.CASE_INSENSITIVE);
	// Ground Marker Variables' own icon of an item, by id
	private static final Pattern ITEM_ICON = Pattern.compile("<item=(\\d+)>", Pattern.CASE_INSENSITIVE);
	private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("h:mm");
	private static final DateTimeFormatter TIME_24 = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH);

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final ItemManager itemManager;

	// the one question put to Ground Marker Variables, which answers it before the post returns
	private String asked;
	private String reply;

	@Inject
	VariableResolver(Client client, ClientThread clientThread, EventBus eventBus, ItemManager itemManager)
	{
		this.itemManager = itemManager;
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
	}

	/**
	 * Whether the text has anything that looks like a variable.
	 */
	static boolean hasVariables(String text)
	{
		return text != null && !tokens(text).isEmpty();
	}

	/**
	 * The text with each variable filled in, on the client thread. One that can't be leaves its braces in.
	 *
	 * @param encode whether each value is made safe to put in a link
	 */
	String resolve(String text, boolean encode)
	{
		return fill(text, this::resolveToken, encode);
	}

	/**
	 * {@link #resolve} from the Swing thread, with the answer back on it.
	 */
	void resolveLater(String text, boolean encode, Consumer<String> then)
	{
		clientThread.invoke(() ->
		{
			final String resolved = resolve(text, encode);
			SwingUtilities.invokeLater(() -> then.accept(resolved));
		});
	}

	@Subscribe
	public void onPluginMessage(PluginMessage event)
	{
		if (NAMESPACE.equals(event.getNamespace()) && RESOLVE.equals(event.getName()) && asked != null
			&& asked.equals(event.getData().get("text")) && event.getData().get("result") instanceof String)
		{
			reply = (String) event.getData().get("result");
		}
	}

	// one "{...}", the palette's own first, then Ground Marker Variables'
	private String resolveToken(String token)
	{
		final String own = resolveNative(token);
		if (own != null)
		{
			return own;
		}
		// the bare value, unless the token asks otherwise, whatever the user's own setting in that plugin
		final String plain = plain(token);
		String answer = askGroundMarkerVariables(plain);
		// a condition or a sum takes no prefix
		final String unprefixed = unprefixed(token);
		if (answer == null && !unprefixed.equals(plain))
		{
			answer = askGroundMarkerVariables(unprefixed);
		}
		return answer;
	}

	// null if it has no answer, or leaves the token as it was
	private String askGroundMarkerVariables(String token)
	{
		asked = token;
		reply = null;
		try
		{
			eventBus.post(new PluginMessage(GMV_NAMESPACE, RESOLVE, Map.of("plugin", NAMESPACE, "text", token)));
		}
		finally
		{
			asked = null;
		}
		return reply == null || reply.equals(token) ? null : readable(reply, this::itemName);
	}

	// on the client thread
	private String itemName(int id)
	{
		return itemManager == null ? "item " + id : itemManager.getItemComposition(id).getName();
	}

	/**
	 * Ground Marker Variables' answer as text to show anywhere: each item icon, "<item=27277>", as the item's name,
	 * its new lines, written "\n", as new lines, and how it styles a tile and its colours left out.
	 *
	 * @param itemName the name of the item with that id
	 */
	static String readable(String answer, IntFunction<String> itemName)
	{
		String text = ITEM_ICON.matcher(answer).replaceAll(match ->
			Matcher.quoteReplacement(itemName.apply(Integer.parseInt(match.group(1)))));
		text = TILE_STYLE.matcher(text).replaceAll("");
		text = Text.removeTags(text.replace("\\n", "\n"));
		final StringBuilder lines = new StringBuilder();
		for (String line : text.split("\n"))
		{
			if (!line.trim().isEmpty())
			{
				lines.append(lines.length() == 0 ? "" : "\n").append(line.trim());
			}
		}
		return lines.toString();
	}

	private String resolveNative(String token)
	{
		final boolean labelled = token.startsWith("{&");
		final String name = token.substring(token.startsWith("{^") || labelled ? 2 : 1, token.length() - 1).trim();
		final Matcher matcher = NATIVE.matcher(name);
		if (!matcher.matches())
		{
			return null;
		}
		final String which = matcher.group(1).toLowerCase(Locale.ROOT);
		switch (which)
		{
			case "time":
				final LocalTime now = LocalTime.now();
				return now.format(TIME_12) + (now.getHour() < 12 ? " am" : " pm");
			case "time24":
				return LocalTime.now().format(TIME_24);
			case "date":
				return label(labelled, "Date", date(LocalDate.now()));
			case "weekday":
				return LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
			default:
				break;
		}

		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return null;
		}
		final Player player = client.getLocalPlayer();
		if (which.equals("rsn"))
		{
			return label(labelled, "RSN", player.getName());
		}
		if (which.equals("world"))
		{
			return label(labelled, "World", String.valueOf(client.getWorld()));
		}

		final String kind = matcher.group(2).toLowerCase(Locale.ROOT);
		final String skillName = matcher.group(3).toLowerCase(Locale.ROOT);
		if (kind.equals("lvl") && skillName.equals("total"))
		{
			return label(labelled, "Total level", String.valueOf(client.getTotalLevel()));
		}
		if (kind.equals("lvl") && skillName.equals("combat"))
		{
			return label(labelled, "Combat level", String.valueOf(player.getCombatLevel()));
		}
		final Skill skill = skill(skillName);
		if (skill == null)
		{
			return null;
		}
		switch (kind)
		{
			case "lvl":
				return label(labelled, skill.getName() + " level", String.valueOf(client.getRealSkillLevel(skill)));
			case "boost":
				return label(labelled, skill.getName() + " boosted", String.valueOf(client.getBoostedSkillLevel(skill)));
			default:
				return label(labelled, skill.getName() + " xp", String.format(Locale.ROOT, "%,d", client.getSkillExperience(skill)));
		}
	}

	static Skill skill(String name)
	{
		for (Skill skill : Skill.values())
		{
			if (skill.getName().equalsIgnoreCase(name))
			{
				return skill;
			}
		}
		return null;
	}

	// "October 16th 2026", as Ground Marker Variables writes it
	static String date(LocalDate day)
	{
		final int d = day.getDayOfMonth();
		final String suffix = d >= 11 && d <= 13 ? "th" : d % 10 == 1 ? "st" : d % 10 == 2 ? "nd" : d % 10 == 3 ? "rd" : "th";
		return day.format(DATE) + suffix + " " + day.getYear();
	}

	private static String label(boolean labelled, String label, String value)
	{
		return labelled ? label + ": " + value : value;
	}

	/**
	 * "{&kc toa}" as "{kc toa}".
	 */
	static String unprefixed(String token)
	{
		return token.length() > 2 && "^&*".indexOf(token.charAt(1)) >= 0 ? "{" + token.substring(2) : token;
	}

	/**
	 * "{kc toa}" as "{^kc toa}", which asks for the bare value; one with a prefix of its own is left be.
	 */
	static String plain(String token)
	{
		return token.length() > 2 && "^&*".indexOf(token.charAt(1)) < 0 ? "{^" + token.substring(1) : token;
	}

	/**
	 * The text with each outermost "{...}" put through the resolver, and left as it is where that has no answer.
	 *
	 * @param encode whether each value is made safe to put in a link
	 */
	static String fill(String text, Function<String, String> resolveToken, boolean encode)
	{
		final StringBuilder filled = new StringBuilder();
		int from = 0;
		for (int[] span : tokens(text))
		{
			filled.append(text, from, span[0]);
			final String token = text.substring(span[0], span[1]);
			final String value = resolveToken.apply(token);
			filled.append(value == null ? token
				: encode ? URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20") : value);
			from = span[1];
		}
		filled.append(text.substring(from));
		return filled.toString();
	}

	/**
	 * Where each outermost "{...}" starts and ends, nested ones inside it and all, leaving out {text} and any brace
	 * that isn't closed.
	 */
	static List<int[]> tokens(String text)
	{
		final List<int[]> tokens = new ArrayList<>();
		int depth = 0;
		int start = -1;
		for (int i = 0; i < text.length(); i++)
		{
			final char c = text.charAt(i);
			if (c == '{')
			{
				if (depth++ == 0)
				{
					start = i;
				}
			}
			else if (c == '}' && depth > 0 && --depth == 0)
			{
				final String token = text.substring(start, i + 1);
				if (token.length() > 2 && !token.equals(CustomCommand.TEXT))
				{
					tokens.add(new int[]{start, i + 1});
				}
			}
		}
		return tokens;
	}
}
