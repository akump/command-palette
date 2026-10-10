package com.commandpalette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class GeTaxTest
{
	@Test
	public void twoPercentRoundedDown()
	{
		assertEquals("Tax 240,000, leaving 11,760,000 gp", GeTax.describe("12m"));
		assertEquals("Tax 1, leaving 98 gp", GeTax.describe("99"));
		assertEquals("Tax 0, leaving 49 gp", GeTax.describe("49"));
	}

	@Test
	public void cappedAtFiveMillion()
	{
		assertEquals(5_000_000, GeTax.tax(250_000_000));
		assertEquals("Tax 5,000,000, leaving 1,995,000,000 gp", GeTax.describe("2b"));
	}

	@Test
	public void thePriceCanBeASum()
	{
		assertEquals("Tax 80,000, leaving 3,920,000 gp", GeTax.describe("12m / 3"));
	}

	@Test
	public void anythingElseIsNotAPrice()
	{
		assertNull(GeTax.describe("whip"));
		assertNull(GeTax.describe("0"));
		assertNull(GeTax.describe("3b"));
	}
}
