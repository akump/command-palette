package com.commandpalette;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class WorldHopperTest
{
	@Test
	public void thePreviousWorldIsTheLastOneLoggedInTo()
	{
		// the first login has nothing before it
		assertEquals(-1, WorldHopper.previous(-1, -1, 302));
		// hopping
		assertEquals(302, WorldHopper.previous(-1, 302, 330));
		assertEquals(330, WorldHopper.previous(302, 330, 302));
		// logging back in to the same world, after a disconnection, keeps the one before
		assertEquals(302, WorldHopper.previous(302, 330, 330));
	}
}
