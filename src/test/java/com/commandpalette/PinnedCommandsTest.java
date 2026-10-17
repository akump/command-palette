package com.commandpalette;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PinnedCommandsTest
{
	@Test
	public void togglingPinsLastAndUnpins()
	{
		assertEquals(List.of("a", "b"), PinnedCommands.toggled(List.of("a"), "b"));
		assertEquals(List.of("b"), PinnedCommands.toggled(List.of("a", "b"), "a"));
	}

	@Test
	public void nothingMoreIsPinnedOnceFull()
	{
		final List<String> full = new ArrayList<>();
		for (int i = 0; i < PinnedCommands.MAX; i++)
		{
			full.add("key " + i);
		}
		assertEquals(full, PinnedCommands.toggled(full, "one more"));
		assertEquals(PinnedCommands.MAX - 1, PinnedCommands.toggled(full, "key 0").size());
	}
}
