package com.commandpalette;

import net.runelite.api.Skill;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class LevelGoalTest
{
	@Test
	public void skillAndLevelInEitherOrder()
	{
		assertEquals(Skill.SLAYER, LevelGoal.parse("92 slayer").getSkill());
		assertEquals(92, LevelGoal.parse("92 slayer").getLevel());
		assertEquals(Skill.SLAYER, LevelGoal.parse("Slayer 92").getSkill());
		assertEquals(92, LevelGoal.parse(" slayer  92 ").getLevel());
	}

	@Test
	public void theStartOfASkillNameIsEnough()
	{
		assertEquals(Skill.RUNECRAFT, LevelGoal.parse("77 rune").getSkill());
		assertEquals(Skill.HITPOINTS, LevelGoal.parse("hit").getSkill());
		// several skills start with s
		assertNull(LevelGoal.parse("92 s"));
	}

	@Test
	public void theLevelIsOptional()
	{
		assertEquals(0, LevelGoal.parse("slayer").getLevel());
	}

	@Test
	public void anythingElseIsNotAGoal()
	{
		assertNull(LevelGoal.parse("92"));
		assertNull(LevelGoal.parse("1 slayer"));
		assertNull(LevelGoal.parse("127 slayer"));
		assertNull(LevelGoal.parse("92 99 slayer"));
		assertNull(LevelGoal.parse("slayer attack"));
		assertNull(LevelGoal.parse("92 overall"));
		assertNull(LevelGoal.parse("tracker"));
	}
}
