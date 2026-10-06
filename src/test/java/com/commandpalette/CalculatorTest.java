package com.commandpalette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class CalculatorTest
{
	private static void assertValue(double expected, String text)
	{
		final Double value = Calculator.evaluate(text);
		if (value == null)
		{
			throw new AssertionError(text + " wasn't taken for a sum");
		}
		assertEquals(expected, value, 1e-6);
	}

	@Test
	public void amountsWithSuffixes()
	{
		assertValue(8333.333333, "35m / 4.2k");
		assertValue(2_500_000_000.0, "1.5b + 1,000M");
		assertValue(1500, "=1.5k");
	}

	@Test
	public void precedenceBracketsAndNegatives()
	{
		assertValue(14, "2 + 3 * 4");
		assertValue(20, "(2 + 3) x 4");
		assertValue(-6, "-2*3");
		assertValue(1, "4 - -2 - 5");
		assertValue(2.5, "10/4");
	}

	@Test
	public void aLoneNumberNeedsAnEqualsSign()
	{
		assertNull(Calculator.evaluate("302"));
		assertNull(Calculator.evaluate("35m"));
		assertValue(302, "= 302");
	}

	@Test
	public void ordinarySearchesAreNotSums()
	{
		assertNull(Calculator.evaluate("loot tracker"));
		assertNull(Calculator.evaluate("world 302"));
		assertNull(Calculator.evaluate("rune 2h"));
		assertNull(Calculator.evaluate("2 +"));
		assertNull(Calculator.evaluate("(2 + 3"));
		assertNull(Calculator.evaluate("1 / 0"));
		assertNull(Calculator.evaluate("1.2.3 + 1"));
		assertNull(Calculator.evaluate(""));
		assertNull(Calculator.evaluate("="));
	}

	@Test
	public void formatsWithAShortFormForBigAmounts()
	{
		assertEquals("14", Calculator.format(14));
		assertEquals("8,333.33", Calculator.format(8333.333333));
		assertEquals("8,333,333.33 (8.33m)", Calculator.format(8333333.333));
		assertEquals("250,000 (250k)", Calculator.format(250000));
		assertEquals("-2,500,000,000 (-2.5b)", Calculator.format(-2.5e9));
		assertEquals("8333333.33", Calculator.plain(8333333.333));
	}
}
