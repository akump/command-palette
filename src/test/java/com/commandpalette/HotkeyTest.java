package com.commandpalette;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JLabel;
import net.runelite.client.config.Keybind;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HotkeyTest
{
	private static final int CTRL = InputEvent.CTRL_DOWN_MASK;
	private static final int CMD = InputEvent.META_DOWN_MASK;
	private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;

	private Keybind saved = new Keybind(KeyEvent.VK_P, 0);
	private boolean requireModifier = true;
	private int platformModifier = CTRL;

	private final Hotkey hotkey = new Hotkey(new CommandPaletteConfig()
	{
		@Override
		public Keybind hotkey()
		{
			return saved;
		}

		@Override
		public boolean requireModifier()
		{
			return requireModifier;
		}
	}, () -> platformModifier);

	private boolean press(int keyCode, int modifiers)
	{
		return hotkey.matches(
			new KeyEvent(new JLabel(), KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, KeyEvent.CHAR_UNDEFINED));
	}

	@Test
	public void usesThePlatformModifier()
	{
		assertTrue(press(KeyEvent.VK_P, CTRL));
		assertFalse(press(KeyEvent.VK_P, CMD));
		assertFalse(press(KeyEvent.VK_P, 0));
		assertFalse(press(KeyEvent.VK_P, CTRL | SHIFT));
		assertFalse(press(KeyEvent.VK_K, CTRL));
		assertEquals(new Keybind(KeyEvent.VK_P, CTRL), hotkey.keybind());

		platformModifier = CMD;
		assertTrue(press(KeyEvent.VK_P, CMD));
		assertFalse(press(KeyEvent.VK_P, CTRL));
		assertFalse(press(KeyEvent.VK_P, CTRL | CMD));
		assertEquals(new Keybind(KeyEvent.VK_P, CMD), hotkey.keybind());
	}

	@Test
	public void onlyTheKeyOfTheSettingIsUsed()
	{
		// what an earlier version saved on a Mac
		saved = new Keybind(KeyEvent.VK_P, CMD);
		assertTrue(press(KeyEvent.VK_P, CTRL));
		assertFalse(press(KeyEvent.VK_P, CMD));
		assertEquals(new Keybind(KeyEvent.VK_P, CTRL), hotkey.keybind());
	}

	@Test
	public void withoutTheModifierItIsAPlainKey()
	{
		requireModifier = false;
		assertFalse(hotkey.hasModifier());
		assertTrue(press(KeyEvent.VK_P, 0));
		assertFalse(press(KeyEvent.VK_P, CTRL));
		assertFalse(press(KeyEvent.VK_P, SHIFT));
		assertEquals(new Keybind(KeyEvent.VK_P, 0), hotkey.keybind());
	}

	@Test
	public void aHotkeyThatIsNotSetMatchesNothing()
	{
		saved = Keybind.NOT_SET;
		assertFalse(press(KeyEvent.VK_UNDEFINED, CTRL));
		assertFalse(press(KeyEvent.VK_CONTROL, CTRL));
		assertEquals(Keybind.NOT_SET, hotkey.keybind());

		// a modifier on its own has no key to use
		saved = Keybind.CTRL;
		assertFalse(press(KeyEvent.VK_CONTROL, CTRL));
		assertEquals(Keybind.NOT_SET, hotkey.keybind());
	}

	@Test
	public void followsPlatformKeys()
	{
		assertEquals(CMD, PlatformModifier.resolve(null, true));
		assertEquals(CTRL, PlatformModifier.resolve(null, false));
		assertEquals(CMD, PlatformModifier.resolve("AUTO", true));
		assertEquals(CTRL, PlatformModifier.resolve("WINDOWS", true));
		assertEquals(CMD, PlatformModifier.resolve("MAC", false));
	}
}
