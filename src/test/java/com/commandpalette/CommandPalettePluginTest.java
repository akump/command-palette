package com.commandpalette;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CommandPalettePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CommandPalettePlugin.class);
		RuneLite.main(args);
	}
}
