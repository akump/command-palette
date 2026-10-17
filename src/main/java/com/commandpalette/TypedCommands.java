package com.commandpalette;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.Icon;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.item.ItemPrice;

/**
 * Commands made from what was typed rather than found by it: a sum, the odds of a dry streak, the tax on a sale,
 * ticks as time, a timer to start, an item to price, a level to reach, and failing all else a wiki search.
 */
class TypedCommands
{
	private static final String WIKI_SEARCH = "https://oldschool.runescape.wiki/?utm_source=runelite&search=";
	private static final Pattern ITEM = Pattern.compile("(?:alch|price)\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern DRY = Pattern.compile("dry\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAX = Pattern.compile("tax\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern TICKS = Pattern.compile("ticks?\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern TIMER = Pattern.compile("timer\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern XP = Pattern.compile("xp\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final int MAX_ITEMS = 5;

	private final Client client;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final SkillIconManager skillIconManager;
	private final CommandPaletteConfig config;
	private final Timers timers;
	private final Answers answers;

	private Runnable onIconLoaded = () -> {};

	@Inject
	TypedCommands(Client client, ClientThread clientThread, ItemManager itemManager, SkillIconManager skillIconManager,
		CommandPaletteConfig config, Timers timers, Answers answers)
	{
		this.answers = answers;
		this.timers = timers;
		this.client = client;
		this.clientThread = clientThread;
		this.itemManager = itemManager;
		this.skillIconManager = skillIconManager;
		this.config = config;
	}

	/**
	 * @param onIconLoaded run, on any thread, when a command's icon has finished loading and wants repainting
	 */
	void setOnIconLoaded(Runnable onIconLoaded)
	{
		this.onIconLoaded = onIconLoaded;
	}

	List<Command> forQuery(String typed)
	{
		final Query query = Query.parse(typed);
		if (query.isWiki())
		{
			return wikiSearch(query.getText());
		}
		if (query.hasPrefix() || query.getText().isEmpty())
		{
			return Collections.emptyList();
		}

		final List<Command> commands = new ArrayList<>();
		if (config.lookupCommands())
		{
			calculation(query.getText(), commands);
			dryStreak(query.getText(), commands);
			answer(TAX, query.getText(), GeTax::describe, Icons.EQUALS, commands);
			answer(TICKS, query.getText(), Durations::convertTicks, Icons.TIMER, commands);
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				items(query.getText(), commands);
				xpToLevel(query.getText(), commands);
			}
		}
		if (config.timerCommands())
		{
			timer(query.getText(), commands);
		}
		if (config.wikiSearch())
		{
			commands.addAll(wikiSearch(query.getText()));
		}
		return commands;
	}

	static List<Command> wikiSearch(String text)
	{
		final String search = text.trim();
		if (search.isEmpty())
		{
			return Collections.emptyList();
		}
		return Collections.singletonList(new Command("Search the wiki for \"" + search + "\"", "", Icons.SEARCH,
			() -> LinkBrowser.browse(WIKI_SEARCH + URLEncoder.encode(search, StandardCharsets.UTF_8))));
	}

	private void calculation(String text, List<Command> out)
	{
		final Double value = Calculator.evaluate(text);
		if (value != null)
		{
			final String answer = text.trim() + " = " + Calculator.format(value);
			out.add(new Command("= " + Calculator.format(value), "Copy", Icons.EQUALS, () ->
			{
				copy(Calculator.plain(value));
				answers.add(answer, Calculator.plain(value), Icons.EQUALS);
			}));
		}
	}

	private void dryStreak(String text, List<Command> out)
	{
		final Matcher matcher = DRY.matcher(text);
		final DryStreak streak = matcher.matches() ? DryStreak.parse(matcher.group(1)) : null;
		if (streak == null)
		{
			return;
		}
		if (streak.hasKills())
		{
			out.add(copying(streak.describeDry(), Icons.DICE));
		}
		out.add(copying(streak.describeMilestones(), Icons.DICE));
	}

	// for a keyword followed by something the function can answer
	private void answer(Pattern pattern, String text, Function<String, String> function, Icon icon,
		List<Command> out)
	{
		final Matcher matcher = pattern.matcher(text);
		final String answer = matcher.matches() ? function.apply(matcher.group(1)) : null;
		if (answer != null)
		{
			out.add(copying(answer, icon));
		}
	}

	private void timer(String text, List<Command> out)
	{
		final Matcher matcher = TIMER.matcher(text);
		final Timers.Request request = matcher.matches() ? Timers.parse(matcher.group(1)) : null;
		if (request == null)
		{
			return;
		}
		out.add(new Command("Start " + request.describe(), "Notify when done", Icons.TIMER, () -> startTimer(request)));
	}

	/**
	 * Starts the timer, and says so in the chat when logged in.
	 */
	void startTimer(Timers.Request request)
	{
		timers.start(request);
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				say("Started " + request.describe());
			}
		});
	}

	// a command that is its own answer, and copies it
	private Command copying(String answer, Icon icon)
	{
		return new Command(answer, "Copy", icon, () ->
		{
			copy(answer);
			answers.add(answer, icon);
		});
	}

	private static void copy(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	private void items(String text, List<Command> out)
	{
		final Matcher matcher = ITEM.matcher(text);
		if (!matcher.matches())
		{
			return;
		}

		final String name = matcher.group(1).trim().toLowerCase(Locale.ROOT);
		final List<ItemPrice> items = itemManager.search(name);
		items.sort(closestTo(name));
		for (ItemPrice item : items.subList(0, Math.min(items.size(), MAX_ITEMS)))
		{
			final AsyncBufferedImage image = itemManager.getImage(item.getId());
			// item images are drawn on the client thread a moment later
			image.onLoaded(onIconLoaded);
			final Icon icon = Icons.of(image);
			out.add(new Command(item.getName(), "Price and alch value", icon, () -> clientThread.invoke(() ->
			{
				final int alch = itemManager.getItemComposition(item.getId()).getHaPrice();
				final String answer = item.getName() + ": GE "
					+ QuantityFormatter.formatNumber(itemManager.getItemPrice(item.getId()))
					+ " gp, high alch " + QuantityFormatter.formatNumber(alch) + " gp";
				say(answer);
				answers.add(answer, icon);
			})));
		}
	}

	// the item called exactly that, then those starting with it, then the shortest names
	static Comparator<ItemPrice> closestTo(String name)
	{
		return Comparator
			.comparing((ItemPrice item) -> !item.getName().equalsIgnoreCase(name))
			.thenComparing(item -> !item.getName().toLowerCase(Locale.ROOT).startsWith(name))
			.thenComparingInt(item -> item.getName().length())
			.thenComparing(ItemPrice::getName);
	}

	private void xpToLevel(String text, List<Command> out)
	{
		final Matcher matcher = XP.matcher(text);
		if (!matcher.matches())
		{
			return;
		}
		final LevelGoal goal = LevelGoal.parse(matcher.group(1));
		if (goal == null)
		{
			return;
		}

		final Skill skill = goal.getSkill();
		final int xp = client.getSkillExperience(skill);
		// with no level given, the next one
		final int level = goal.getLevel() > 0 ? goal.getLevel()
			: Math.min(Experience.getLevelForXp(xp) + 1, Experience.MAX_VIRT_LEVEL);
		final int remaining = Experience.getXpForLevel(level) - xp;
		final String answer = remaining > 0
			? skill.getName() + ": " + QuantityFormatter.formatNumber(remaining) + " xp to level " + level
			: skill.getName() + " is already level " + level + " or higher";
		final Icon icon = Icons.of(skillIconManager.getSkillImage(skill, true));
		out.add(new Command(answer, "Show in chat", icon, () ->
		{
			answers.add(answer, icon);
			clientThread.invoke(() -> say(answer));
		}));
	}

	// on the client thread
	private void say(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}
}
