package com.commandpalette;

import java.util.List;

/**
 * The variables to pick from in the Custom tab. Ground Marker Variables' are a selection of its own, which its
 * README lists in full.
 */
final class VariableCatalog
{
	static final class Variable
	{
		private final String token;
		private final String description;

		Variable(String token, String description)
		{
			this.token = token;
			this.description = description;
		}

		/**
		 * What is put in the command, an example where the variable takes a name: "{kc zulrah}".
		 */
		String getToken()
		{
			return token;
		}

		String getDescription()
		{
			return description;
		}
	}

	/**
	 * What is typed after the command's name, which is filled in before the rest.
	 */
	static final Variable TEXT = new Variable(CustomCommand.TEXT,
		"What you type after the command's name: ge abyssal whip");

	static final List<Variable> OWN = List.of(
		new Variable("{rsn}", "Your name"),
		new Variable("{world}", "The world you are on"),
		new Variable("{time}", "The time, 8:21 pm; {time24} for 20:21"),
		new Variable("{date}", "Today, October 16th 2026"),
		new Variable("{weekday}", "The day of the week"),
		new Variable("{lvl_agility}", "A skill's level, any skill by name"),
		new Variable("{boost_agility}", "A skill's level as boosted or drained"),
		new Variable("{xp_agility}", "A skill's xp"),
		new Variable("{lvl_total}", "Your total level"),
		new Variable("{lvl_combat}", "Your combat level"));

	static final List<Variable> GROUND_MARKER_VARIABLES = List.of(
		new Variable("{kc zulrah}", "A boss's kill count"),
		new Variable("{pb zulrah}", "Your personal best at a boss"),
		new Variable("{rank zulrah}", "Your hiscores rank at a boss"),
		new Variable("{&loot Tombs of Amascut}", "A monster's loot: its kills, gp and best items, from the Loot"
			+ " Tracker plugin"),
		new Variable("{slayerTask}", "Your slayer task, from the Slayer plugin"),
		new Variable("{slayerStreak}", "Slayer tasks done in a row"),
		new Variable("{slayerPoints}", "Your slayer points"),
		new Variable("{xpRate_agility}", "Xp an hour in a skill, from the XP Tracker plugin"),
		new Variable("{numItem shark}", "How many of an item you carry"),
		new Variable("{numDoses prayer}", "Potion doses you carry"),
		new Variable("{weapon}", "Your weapon"),
		new Variable("{spellbook}", "Your spellbook"),
		new Variable("{questPoints}", "Your quest points"),
		new Variable("{kc tob + kc hmt}", "A sum of variables or numbers"),
		new Variable("{lvl_prayer >= 70 ? Piety : No piety}", "One text or another, by a condition"));

	private VariableCatalog()
	{
	}
}
