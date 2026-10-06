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
 * Commands made from what was typed rather than found by it: a sum, the odds of a dry streak, an item to price,
 * a level to reach, and
 * failing all else a wiki search.
 */
class TypedCommands
{
	private static final String WIKI_SEARCH = "https://oldschool.runescape.wiki/?utm_source=runelite&search=";
	private static final Pattern ITEM = Pattern.compile("(?:alch|price)\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern DRY = Pattern.compile("dry\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern XP = Pattern.compile("xp\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final int MAX_ITEMS = 5;

	private final Client client;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final SkillIconManager skillIconManager;
	private final CommandPaletteConfig config;

	private Runnable onIconLoaded = () -> {};

	@Inject
	TypedCommands(Client client, ClientThread clientThread, ItemManager itemManager, SkillIconManager skillIconManager,
		CommandPaletteConfig config)
	{
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
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				items(query.getText(), commands);
				xpToLevel(query.getText(), commands);
			}
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

	private static void calculation(String text, List<Command> out)
	{
		final Double value = Calculator.evaluate(text);
		if (value != null)
		{
			out.add(new Command("= " + Calculator.format(value), "Copy", Icons.EQUALS, () -> Toolkit.getDefaultToolkit()
				.getSystemClipboard().setContents(new StringSelection(Calculator.plain(value)), null)));
		}
	}

	private static void dryStreak(String text, List<Command> out)
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

	// a command that is its own answer, and copies it
	private static Command copying(String answer, Icon icon)
	{
		return new Command(answer, "Copy", icon, () -> Toolkit.getDefaultToolkit()
			.getSystemClipboard().setContents(new StringSelection(answer), null));
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
			out.add(new Command(item.getName(), "Price and alch value", Icons.of(image), () -> clientThread.invoke(() ->
			{
				final int alch = itemManager.getItemComposition(item.getId()).getHaPrice();
				say(item.getName() + ": GE " + QuantityFormatter.formatNumber(itemManager.getItemPrice(item.getId()))
					+ " gp, high alch " + QuantityFormatter.formatNumber(alch) + " gp");
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
		out.add(new Command(answer, "Show in chat", Icons.of(skillIconManager.getSkillImage(skill, true)),
			() -> clientThread.invoke(() -> say(answer))));
	}

	// on the client thread
	private void say(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}
}
