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
	public void recentCommandsArePickedLatestFirst()
	{
		final List<Command> commands = Arrays.asList(command("a"), command("b"), command(null), command("c"), command("d"));
		assertEquals(List.of("d", "b"), titles(RecentCommands.pick(commands, List.of("d", "gone", "b"), 8)));
	}

	@Test
	public void noMoreThanTheLimitArePicked()
	{
		final List<Command> commands = Arrays.asList(command("a"), command("b"), command("c"));
		assertEquals(List.of("c", "a"), titles(RecentCommands.pick(commands, List.of("c", "gone", "a", "b"), 2)));
	}

	@Test
	public void nothingRecentPicksNothing()
	{
		assertEquals(List.of(), RecentCommands.pick(Arrays.asList(command("a"), command("b")), List.of(), 8));
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
