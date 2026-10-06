package com.commandpalette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class DryStreakTest
{
	@Test
	public void rateAndKillsInEitherOrder()
	{
		assertEquals("30.9% chance of going 600 dry at 1/512", DryStreak.parse("1/512 600").describeDry());
		assertEquals("30.9% chance of going 600 dry at 1/512", DryStreak.parse(" 600  1/512 ").describeDry());
	}

	@Test
	public void twoPlainNumbersAreTheRateThenTheKills()
	{
		assertEquals("30.9% chance of going 600 dry at 1/512", DryStreak.parse("512 600").describeDry());
	}

	@Test
	public void milestones()
	{
		final DryStreak streak = DryStreak.parse("1/512");
		assertFalse(streak.hasKills());
		assertEquals("1/512: 50% by 355 kills, 90% by 1,178", streak.describeMilestones());
		// half of players have it by then, and not one kill sooner
		assertEquals(355, streak.killsFor(0.5));
		assertEquals(1, DryStreak.parse("1/2").killsFor(0.5));
	}

	@Test
	public void otherWaysOfWritingIt()
	{
		assertEquals("9.3% chance of going 100 dry at 3/128", DryStreak.parse("3/128 100").describeDry());
		assertEquals("0.74% chance of going 5,000 dry at 1/1020.5", DryStreak.parse("1/1020.5 5k").describeDry());
		assertEquals("14.2% chance of going 1,000 dry at 1/512", DryStreak.parse("1/512 1,000").describeDry());
		assertEquals("100% chance of going 0 dry at 1/512", DryStreak.parse("1/512 0").describeDry());
	}

	@Test
	public void percentages()
	{
		assertEquals("50%", DryStreak.percent(0.5));
		assertEquals("9.4%", DryStreak.percent(0.0941));
		assertEquals("0.42%", DryStreak.percent(0.00416));
		assertEquals("0.0031%", DryStreak.percent(0.0000312));
		assertEquals("under 0.0001%", DryStreak.percent(1e-9));
	}

	@Test
	public void anythingElseIsNotADryStreak()
	{
		assertNull(DryStreak.parse("512"));
		assertNull(DryStreak.parse("spell"));
		assertNull(DryStreak.parse("1/512 600 700"));
		assertNull(DryStreak.parse("1/512 1/128"));
		assertNull(DryStreak.parse("1/0 100"));
		assertNull(DryStreak.parse("0/512 100"));
		assertNull(DryStreak.parse("2/1 100"));
		assertNull(DryStreak.parse("1 600"));
		assertNull(DryStreak.parse("1/512 lots"));
		assertNull(DryStreak.parse(""));
	}
}
