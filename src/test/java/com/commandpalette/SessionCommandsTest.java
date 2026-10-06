package com.commandpalette;

import java.time.Duration;
import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SessionCommandsTest
{
	private static int[] xp(Object... skillsAndXp)
	{
		final int[] xp = new int[Skill.values().length];
		for (int i = 0; i < skillsAndXp.length; i += 2)
		{
			xp[((Skill) skillsAndXp[i]).ordinal()] = (Integer) skillsAndXp[i + 1];
		}
		return xp;
	}

	@Test
	public void nothingGained()
	{
		assertEquals("Session: 5m logged in, 0 xp gained",
			SessionCommands.summarize(Duration.ofSeconds(359), xp(Skill.SLAYER, 100), xp(Skill.SLAYER, 100)));
	}

	@Test
	public void listsTheBiggestGainsFirst()
	{
		assertEquals("Session: 1h 23m logged in, 45,678 xp gained (Slayer 30,000, Hitpoints 10,000, Attack 5,678)",
			SessionCommands.summarize(Duration.ofMinutes(83),
				xp(Skill.ATTACK, 1000, Skill.HITPOINTS, 5000, Skill.SLAYER, 0),
				xp(Skill.ATTACK, 6678, Skill.HITPOINTS, 15000, Skill.SLAYER, 30000)));
	}

	@Test
	public void onlyTheTopThreeSkillsAreNamed()
	{
		assertEquals("Session: 2h 0m logged in, 1,111 xp gained (Slayer 1,000, Attack 100, Magic 10, ...)",
			SessionCommands.summarize(Duration.ofHours(2), xp(),
				xp(Skill.COOKING, 1, Skill.MAGIC, 10, Skill.ATTACK, 100, Skill.SLAYER, 1000)));
	}
}
