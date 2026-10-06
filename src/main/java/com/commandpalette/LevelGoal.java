package com.commandpalette;

import java.util.Locale;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * A skill and optionally a level to reach in it, as typed: "92 slayer", "slay 92", "runecraft".
 */
final class LevelGoal
{
	private final Skill skill;
	private final int level;

	private LevelGoal(Skill skill, int level)
	{
		this.skill = skill;
		this.level = level;
	}

	/**
	 * @return null unless the text is one skill, by name or the start of it, with at most one level from 2 to 126
	 */
	static LevelGoal parse(String text)
	{
		Skill skill = null;
		int level = 0;
		for (String word : text.trim().toLowerCase(Locale.ROOT).split("\\s+"))
		{
			if (word.matches("\\d{1,3}"))
			{
				final int number = Integer.parseInt(word);
				if (level != 0 || number < 2 || number > Experience.MAX_VIRT_LEVEL)
				{
					return null;
				}
				level = number;
			}
			else
			{
				final Skill named = skillStarting(word);
				if (skill != null || named == null)
				{
					return null;
				}
				skill = named;
			}
		}
		return skill == null ? null : new LevelGoal(skill, level);
	}

	private static Skill skillStarting(String word)
	{
		Skill found = null;
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL && skill.getName().toLowerCase(Locale.ROOT).startsWith(word))
			{
				if (found != null)
				{
					// "s" could be several skills
					return null;
				}
				found = skill;
			}
		}
		return found;
	}

	Skill getSkill()
	{
		return skill;
	}

	/**
	 * The level asked for, or 0 if none was.
	 */
	int getLevel()
	{
		return level;
	}
}
