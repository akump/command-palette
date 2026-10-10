package com.commandpalette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class DurationsTest
{
	@Test
	public void timesAsTyped()
	{
		assertEquals(300, Durations.parse("5m"), 0);
		assertEquals(150, Durations.parse("2m30s"), 0);
		assertEquals(5400, Durations.parse(" 1H 30 min "), 0);
		assertEquals(90, Durations.parse("1.5 minutes"), 0);
		assertEquals(90, Durations.parse("1:30"), 0);
		assertEquals(3723, Durations.parse("1:02:03"), 0);
	}

	@Test
	public void anythingElseIsNotATime()
	{
		assertEquals(-1, Durations.parse("5"), 0);
		assertEquals(-1, Durations.parse("5 marks"), 0);
		assertEquals(-1, Durations.parse("m"), 0);
		assertEquals(-1, Durations.parse("5m herb"), 0);
		assertEquals(-1, Durations.parse(""), 0);
	}

	@Test
	public void formatting()
	{
		assertEquals("2m 30s", Durations.format(150));
		assertEquals("1h 5m", Durations.format(3900));
		assertEquals("1h 0.6s", Durations.format(3600.6));
		assertEquals("0s", Durations.format(0));
	}

	@Test
	public void ticksEitherWay()
	{
		assertEquals("250 ticks = 2m 30s", Durations.convertTicks("250"));
		assertEquals("1 tick = 0.6s", Durations.convertTicks("1"));
		assertEquals("1,000 ticks = 10m", Durations.convertTicks("1,000"));
		assertEquals("2m 30s = 250 ticks", Durations.convertTicks("2m30s"));
		assertEquals("1s = 1.67 ticks", Durations.convertTicks("1s"));
		assertEquals("0.6s = 1 tick", Durations.convertTicks("0.6s"));
		assertNull(Durations.convertTicks("lots"));
		assertNull(Durations.convertTicks("2.5"));
	}
}
