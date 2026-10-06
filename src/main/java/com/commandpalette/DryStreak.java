package com.commandpalette;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The odds of going without a drop, from a drop rate and optionally a number of kills as typed: "1/512 600",
 * "600 1/512", "512 600", "3/128".
 */
final class DryStreak
{
	private static final Pattern RATE = Pattern.compile("(\\d+(?:\\.\\d+)?)/(\\d+(?:\\.\\d+)?)");
	private static final Pattern COUNT = Pattern.compile("(\\d+(?:\\.\\d+)?)([kKmM]?)");
	private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ROOT);

	private final String rate;
	private final double chance;
	private final long kills;

	private DryStreak(String rate, double chance, long kills)
	{
		this.rate = rate;
		this.chance = chance;
		this.kills = kills;
	}

	/**
	 * @return null unless the text is a drop rate, with or without a number of kills. A rate is written 1/512;
	 * given two plain numbers, the first is taken as the 512.
	 */
	static DryStreak parse(String text)
	{
		String rate = null;
		double chance = 0;
		double kills = -1;
		final String[] words = text.trim().replace(",", "").split("\\s+");
		if (words.length > 2)
		{
			return null;
		}
		for (String word : words)
		{
			final Matcher fraction = RATE.matcher(word);
			final Matcher count = COUNT.matcher(word);
			if (fraction.matches() && rate == null)
			{
				rate = word;
				chance = Double.parseDouble(fraction.group(1)) / Double.parseDouble(fraction.group(2));
			}
			else if (count.matches() && kills < 0)
			{
				final String suffix = count.group(2).toLowerCase(Locale.ROOT);
				kills = Double.parseDouble(count.group(1)) * (suffix.equals("k") ? 1e3 : suffix.equals("m") ? 1e6 : 1);
			}
			else if (count.matches() && rate == null)
			{
				// two plain numbers: the one before was the rate after all
				rate = "1/" + words[0];
				chance = 1 / Double.parseDouble(words[0]);
				kills = Double.parseDouble(count.group(1));
				if (!COUNT.matcher(words[0]).matches() || !count.group(2).isEmpty() || words[0].matches(".*[kKmM]"))
				{
					return null;
				}
			}
			else
			{
				return null;
			}
		}
		// NaN from 0/0 fails this too
		if (rate == null || !(chance > 0 && chance < 1))
		{
			return null;
		}
		return new DryStreak(rate, chance, kills < 0 ? -1 : Math.round(kills));
	}

	boolean hasKills()
	{
		return kills >= 0;
	}

	/**
	 * The chance of not having the drop after that many kills.
	 */
	double chanceDry()
	{
		return Math.pow(1 - chance, kills);
	}

	/**
	 * How many kills it takes for that share of players, from 0 to 1, to have had the drop.
	 */
	long killsFor(double share)
	{
		return (long) Math.ceil(Math.log(1 - share) / Math.log(1 - chance));
	}

	/**
	 * "30.9% chance of going 600 dry at 1/512"
	 */
	String describeDry()
	{
		return percent(chanceDry()) + " chance of going " + number(kills) + " dry at " + rate;
	}

	/**
	 * "1/512: 50% by 355 kills, 90% by 1,178"
	 */
	String describeMilestones()
	{
		return rate + ": 50% by " + number(killsFor(0.5)) + " kills, 90% by " + number(killsFor(0.9));
	}

	// one decimal for everyday chances, two significant figures for small ones
	static String percent(double share)
	{
		final double percent = share * 100;
		if (percent >= 10)
		{
			return new DecimalFormat("0.#", SYMBOLS).format(percent) + "%";
		}
		if (percent < 0.0001)
		{
			return "under 0.0001%";
		}
		final int decimals = Math.max(1, 1 - (int) Math.floor(Math.log10(percent)));
		return new DecimalFormat("0." + "#".repeat(decimals), SYMBOLS).format(percent) + "%";
	}

	private static String number(long value)
	{
		return new DecimalFormat("#,##0", SYMBOLS).format(value);
	}
}
