package com.commandpalette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class TimersTest
{
	@Test
	public void aTimeAndWhatItIsFor()
	{
		assertEquals("5m timer: herb run", Timers.parse("5m herb run").describe());
		assertEquals("1h 30m timer: birdhouses", Timers.parse("1h 30m birdhouses").describe());
		assertEquals("1m 30s timer", Timers.parse("1:30").describe());
	}

	@Test
	public void aNumberOnItsOwnIsMinutes()
	{
		assertEquals("5m timer", Timers.parse("5").describe());
		assertEquals("5m timer: marks of grace", Timers.parse("5 marks of grace").describe());
	}

	@Test
	public void anythingElseIsNotATimer()
	{
		assertNull(Timers.parse("herb run"));
		assertNull(Timers.parse("herb run 5m"));
		assertNull(Timers.parse("0s"));
		assertNull(Timers.parse("25h"));
		assertNull(Timers.parse(""));
	}
}
