package com.commandpalette;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lengths of time the way they are typed, "5m", "2m30s", "1h 30m" or "1:30", and game ticks of 0.6 seconds.
 */
final class Durations
{
	static final double TICK_SECONDS = 0.6;

	private static final Pattern PART = Pattern.compile(
		"(\\d+(?:\\.\\d+)?)\\s*(h|hrs?|hours?|m|mins?|minutes?|s|secs?|seconds?)(?![a-z])\\s*");
	private static final Pattern CLOCK = Pattern.compile("(?:(\\d+):)?(\\d+):(\\d{2})");
	private static final Pattern TICKS = Pattern.compile("\\d{1,9}");
	private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ROOT);

	private Durations()
	{
	}

	/**
	 * @return the seconds in the text, or -1 if it isn't a length of time. A number needs its unit, or a colon as
	 * in 1:30.
	 */
	static double parse(String text)
	{
		final String typed = text.trim().toLowerCase(Locale.ROOT);
		final Matcher clock = CLOCK.matcher(typed);
		if (clock.matches())
		{
			final double hours = clock.group(1) == null ? 0 : Double.parseDouble(clock.group(1));
			return hours * 3600 + Double.parseDouble(clock.group(2)) * 60 + Double.parseDouble(clock.group(3));
		}

		double seconds = 0;
		int end = 0;
		final Matcher part = PART.matcher(typed);
		while (end < typed.length() && part.region(end, typed.length()).lookingAt())
		{
			final char unit = part.group(2).charAt(0);
			seconds += Double.parseDouble(part.group(1)) * (unit == 'h' ? 3600 : unit == 'm' ? 60 : 1);
			end = part.end();
		}
		return end > 0 && end == typed.length() ? seconds : -1;
	}

	/**
	 * "2m 30s", "1h 5m", "0.6s"
	 */
	static String format(double seconds)
	{
		final long tenths = Math.round(seconds * 10);
		final long hours = tenths / 36000;
		final long minutes = tenths % 36000 / 600;
		final double rest = tenths % 600 / 10.0;
		final List<String> parts = new ArrayList<>();
		if (hours > 0)
		{
			parts.add(hours + "h");
		}
		if (minutes > 0)
		{
			parts.add(minutes + "m");
		}
		if (rest > 0 || parts.isEmpty())
		{
			parts.add(new DecimalFormat("0.#", SYMBOLS).format(rest) + "s");
		}
		return String.join(" ", parts);
	}

	/**
	 * A number of ticks as the time they take, "250 ticks = 2m 30s", or a time as the ticks in it,
	 * "2m 30s = 250 ticks".
	 *
	 * @return null if the text is neither
	 */
	static String convertTicks(String text)
	{
		final String typed = text.trim().replace(",", "");
		if (TICKS.matcher(typed).matches())
		{
			final long ticks = Long.parseLong(typed);
			return ticks(ticks) + " = " + format(ticks * TICK_SECONDS);
		}
		final double seconds = parse(typed);
		return seconds < 0 ? null : format(seconds) + " = " + ticks(seconds / TICK_SECONDS);
	}

	private static String ticks(double ticks)
	{
		final String number = new DecimalFormat("#,##0.##", SYMBOLS).format(ticks);
		return number + (number.equals("1") ? " tick" : " ticks");
	}
}
