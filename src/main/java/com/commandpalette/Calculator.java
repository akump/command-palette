package com.commandpalette;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Arithmetic the way amounts are written in game: 35m / 4.2k, (1.5b - 250m) x 2.
 */
final class Calculator
{
	private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ROOT);

	private final String input;
	private int pos;
	private int operators;

	private Calculator(String input)
	{
		this.input = input;
	}

	/**
	 * The value of the expression, or null if the text isn't one. A lone number only counts after an equals sign,
	 * so that typing a world number isn't taken for a sum.
	 */
	static Double evaluate(String text)
	{
		String expression = text.trim();
		final boolean explicit = expression.startsWith("=");
		if (explicit)
		{
			expression = expression.substring(1);
		}

		final Calculator calculator = new Calculator(expression);
		try
		{
			final double value = calculator.sum();
			calculator.skipSpaces();
			if (calculator.pos != expression.length() || (!explicit && calculator.operators == 0)
				|| Double.isNaN(value) || Double.isInfinite(value))
			{
				return null;
			}
			return value;
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	/**
	 * With thousands separators and at most two decimals, followed by the short form for big amounts:
	 * 8,333,333.33 (8.33m)
	 */
	static String format(double value)
	{
		final String full = new DecimalFormat("#,##0.##", SYMBOLS).format(value);
		final double size = Math.abs(value);
		if (size < 100_000)
		{
			return full;
		}
		final DecimalFormat brief = new DecimalFormat("0.##", SYMBOLS);
		final String shortened = size >= 1e9 ? brief.format(value / 1e9) + "b"
			: size >= 1e6 ? brief.format(value / 1e6) + "m"
			: brief.format(value / 1e3) + "k";
		return full + " (" + shortened + ")";
	}

	/**
	 * Just the digits, for pasting elsewhere.
	 */
	static String plain(double value)
	{
		return new DecimalFormat("0.##", SYMBOLS).format(value);
	}

	private double sum()
	{
		double value = product();
		while (true)
		{
			if (take('+'))
			{
				value += product();
			}
			else if (take('-'))
			{
				value -= product();
			}
			else
			{
				return value;
			}
		}
	}

	private double product()
	{
		double value = factor();
		while (true)
		{
			if (take('*') || take('x') || take('X'))
			{
				value *= factor();
			}
			else if (take('/'))
			{
				value /= factor();
			}
			else
			{
				return value;
			}
		}
	}

	private double factor()
	{
		skipSpaces();
		if (peek() == '-')
		{
			pos++;
			return -factor();
		}
		if (peek() == '(')
		{
			pos++;
			final double value = sum();
			skipSpaces();
			if (peek() != ')')
			{
				throw new IllegalArgumentException("unclosed bracket");
			}
			pos++;
			return value;
		}
		return number();
	}

	private double number()
	{
		final StringBuilder digits = new StringBuilder();
		while (Character.isDigit(peek()) || peek() == '.' || peek() == ',')
		{
			if (peek() != ',')
			{
				digits.append(peek());
			}
			pos++;
		}
		// throws NumberFormatException, an IllegalArgumentException, on anything that isn't a number
		double value = Double.parseDouble(digits.toString());
		switch (Character.toLowerCase(peek()))
		{
			case 'k':
				value *= 1e3;
				pos++;
				break;
			case 'm':
				value *= 1e6;
				pos++;
				break;
			case 'b':
				value *= 1e9;
				pos++;
				break;
			default:
				break;
		}
		return value;
	}

	// a binary operator
	private boolean take(char operator)
	{
		skipSpaces();
		if (peek() != operator)
		{
			return false;
		}
		pos++;
		operators++;
		return true;
	}

	private void skipSpaces()
	{
		while (peek() == ' ')
		{
			pos++;
		}
	}

	private char peek()
	{
		return pos < input.length() ? input.charAt(pos) : 0;
	}
}
