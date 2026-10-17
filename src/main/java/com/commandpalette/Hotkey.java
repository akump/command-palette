package com.commandpalette;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.IntSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.Keybind;

/**
 * The key that opens the palette. Only the key of the hotkey setting is used: the modifier is the platform's own,
 * Cmd on a Mac and Ctrl elsewhere, or none at all when the setting that asks for it is off.
 */
@Singleton
class Hotkey
{
	private static final int MODIFIER_MASK = InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK
		| InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

	private final CommandPaletteConfig config;
	private final IntSupplier platformModifier;

	@Inject
	Hotkey(CommandPaletteConfig config, PlatformModifier platformModifier)
	{
		this(config, platformModifier::get);
	}

	Hotkey(CommandPaletteConfig config, IntSupplier platformModifier)
	{
		this.config = config;
		this.platformModifier = platformModifier;
	}

	boolean hasModifier()
	{
		return config.requireModifier();
	}

	boolean matches(KeyEvent e)
	{
		final int code = keyCode(e);
		return code != KeyEvent.VK_UNDEFINED && code == config.hotkey().getKeyCode()
			&& (e.getModifiersEx() & MODIFIER_MASK) == modifier();
	}

	/**
	 * The keys to press, modifier included.
	 */
	Keybind keybind()
	{
		final int code = config.hotkey().getKeyCode();
		return code == KeyEvent.VK_UNDEFINED ? Keybind.NOT_SET : new Keybind(code, modifier());
	}

	/**
	 * The keys to press, as the plugin's own text says them, "Cmd+P" or "Ctrl+P", or null if the key is unset or
	 * has no plain name: the client's names, such as a Mac's ⌘, aren't all in RuneLite's fonts.
	 */
	String describe()
	{
		final int code = config.hotkey().getKeyCode();
		return code == KeyEvent.VK_UNDEFINED ? null : describe(modifier(), KeyEvent.getKeyText(code));
	}

	static String describe(int modifier, String key)
	{
		if (!key.chars().allMatch(c -> c >= ' ' && c < 127))
		{
			return null;
		}
		final String prefix = modifier == InputEvent.META_DOWN_MASK ? "Cmd+"
			: modifier == InputEvent.CTRL_DOWN_MASK ? "Ctrl+" : "";
		return prefix + key;
	}

	private int modifier()
	{
		return hasModifier() ? platformModifier.getAsInt() : 0;
	}

	// The extended code is the one a keybind records, and an event that wasn't typed on a keyboard has none
	private static int keyCode(KeyEvent e)
	{
		final int extended = e.getExtendedKeyCode();
		return extended != KeyEvent.VK_UNDEFINED ? extended : e.getKeyCode();
	}
}
