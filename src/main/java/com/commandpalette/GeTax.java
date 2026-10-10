package com.commandpalette;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * The Grand Exchange's tax on selling an item: 2% of its price rounded down, and no more than 5m.
 */
final class GeTax
{
	private static final double RATE = 0.02;
	private static final long CAP = 5_000_000;
	private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ROOT);

	private GeTax()
	{
	}

	static long tax(long price)
	{
		return Math.min(CAP, (long) Math.floor(price * RATE));
	}

	/**
	 * "Tax 240,000, leaving 11,760,000 gp"
	 *
	 * @param text the price of one item, as a sum: "12m", "1.2b / 3"
	 * @return null if the text isn't a price that an item can sell for
	 */
	static String describe(String text)
	{
		final Double value = Calculator.evaluate("=" + text);
		if (value == null || value < 1 || value > Integer.MAX_VALUE)
		{
			return null;
		}
		final long price = (long) Math.floor(value);
		final long tax = tax(price);
		return "Tax " + number(tax) + ", leaving " + number(price - tax) + " gp";
	}

	private static String number(long value)
	{
		return new DecimalFormat("#,##0", SYMBOLS).format(value);
	}
}
