package com.commandpalette;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RecentCommandsTest
{
	private static Command command(String key)
	{
		return new Command(key, key == null ? "unkeyed" : key, "", null, () -> {});
	}

	private static List<String> titles(List<Command> commands)
	{
		return commands.stream().map(Command::getTitle).collect(Collectors.toList());
	}

	@Test
	public void recentCommandsComeFirstLatestFirst()
	{
		final List<Command> commands = Arrays.asList(command("a"), command("b"), command(null), command("c"), command("d"));
		assertEquals(List.of("d", "b", "a", "unkeyed", "c"),
			titles(RecentCommands.recentFirst(commands, List.of("d", "gone", "b"))));
	}

	@Test
	public void nothingRecentLeavesTheOrderAlone()
	{
		final List<Command> commands = Arrays.asList(command("a"), command("b"));
		assertEquals(commands, RecentCommands.recentFirst(commands, List.of()));
	}

	@Test
	public void runningACommandAgainMovesItToTheFront()
	{
		assertEquals(List.of("b", "a", "c"), RecentCommands.withLatest(List.of("a", "b", "c"), "b"));
	}

	@Test
	public void onlyTheLastFewAreKept()
	{
		List<String> recent = List.of();
		for (int i = 1; i <= RecentCommands.MAX + 2; i++)
		{
			recent = RecentCommands.withLatest(recent, "k" + i);
		}
		assertEquals(RecentCommands.MAX, recent.size());
		assertEquals("k" + (RecentCommands.MAX + 2), recent.get(0));
		assertEquals("k3", recent.get(recent.size() - 1));
	}
}
