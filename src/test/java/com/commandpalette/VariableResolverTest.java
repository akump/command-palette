package com.commandpalette;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class VariableResolverTest
{
	// answers the way Ground Marker Variables does, within the post
	public static final class FakeGroundMarkerVariables
	{
		private final EventBus eventBus;
		private final Map<String, String> answers;

		FakeGroundMarkerVariables(EventBus eventBus, Map<String, String> answers)
		{
			this.eventBus = eventBus;
			this.answers = answers;
		}

		@Subscribe
		public void onPluginMessage(PluginMessage event)
		{
			if (!VariableResolver.GMV_NAMESPACE.equals(event.getNamespace()))
			{
				return;
			}
			final String text = (String) event.getData().get("text");
			eventBus.post(new PluginMessage((String) event.getData().get("plugin"), VariableResolver.RESOLVE,
				Map.of("text", text, "result", answers.getOrDefault(text, text))));
		}
	}

	@Test
	public void tokensAreTheOutermostBraces()
	{
		final String text = "{rsn} has {if {lvl_agility} > 70 ? fast : slow} {text} {unclosed";
		final List<int[]> tokens = VariableResolver.tokens(text);
		assertEquals(2, tokens.size());
		assertEquals("{rsn}", text.substring(tokens.get(0)[0], tokens.get(0)[1]));
		assertEquals("{if {lvl_agility} > 70 ? fast : slow}", text.substring(tokens.get(1)[0], tokens.get(1)[1]));
		assertFalse(VariableResolver.hasVariables("ge {text} and {}"));
		assertTrue(VariableResolver.hasVariables("{kc toa}"));
	}

	@Test
	public void eachTokenIsFilledInOrLeftAsItWas()
	{
		final Map<String, String> values = Map.of("{rsn}", "Zezima Two", "{kc toa}", "58");
		assertEquals("Zezima Two: 58 toa, {nope}", VariableResolver.fill("{rsn}: {kc toa} toa, {nope}", values::get,
			false));
		assertEquals("https://x.com/p?n=Zezima%20Two&q={text}",
			VariableResolver.fill("https://x.com/p?n={rsn}&q={text}", values::get, true));
	}

	@Test
	public void theBareValueIsAskedForUnlessAnotherIs()
	{
		assertEquals("{^kc toa}", VariableResolver.plain("{kc toa}"));
		assertEquals("{&kc toa}", VariableResolver.plain("{&kc toa}"));
		assertEquals("{^rsn}", VariableResolver.plain("{^rsn}"));
	}

	@Test
	public void groundMarkerVariablesFillsInWhatThePaletteCant()
	{
		final EventBus eventBus = new EventBus();
		final VariableResolver resolver = new VariableResolver(null, null, eventBus, null);
		eventBus.register(resolver);
		assertEquals("{kc toa}", resolver.resolve("{kc toa}", false));

		eventBus.register(new FakeGroundMarkerVariables(eventBus, Map.of(
			"{^kc toa}", "58",
			// a conditional takes no prefix, so is asked for as it is
			"{if x ? <col=ff0000>a</col> : b}", "<col=ff0000>a</col>")));
		assertEquals("58 / a / {kc nothing}",
			resolver.resolve("{kc toa} / {if x ? <col=ff0000>a</col> : b} / {kc nothing}", false));
	}

	@Test
	public void lootIsShownWithItsItemsNamedAndItsLinesKept()
	{
		final java.util.Map<Integer, String> items = java.util.Map.of(27277, "Tumeken's shadow", 25985, "Elidinis' ward");
		assertEquals("Tombs of Amascut (58): 12.3M | 1x Tumeken's shadow 2x Elidinis' ward",
			VariableResolver.readable("Tombs of Amascut (<col=ffff00>58</col>): <col=ffff00>12.3M</col> | 1x <item=27277>"
				+ " 2x <item=25985>", items::get));
		assertEquals("Tombs of Amascut (58): 12.3M\n1x Tumeken's shadow",
			VariableResolver.readable("{align=left}Tombs of Amascut (58): 12.3M\\n1x <item=27277>\\n{/align}",
				items::get));
	}

	@Test
	public void aConditionIsAskedForWithoutThePrefixItTakesNone()
	{
		final EventBus eventBus = new EventBus();
		final VariableResolver resolver = new VariableResolver(null, null, eventBus, null);
		eventBus.register(resolver);
		eventBus.register(new FakeGroundMarkerVariables(eventBus, Map.of("{x > 1 ? yes : no}", "yes",
			"{&loot toa}", "Tombs of Amascut (58): 12.3M")));
		assertEquals("yes / Tombs of Amascut (58): 12.3M", resolver.resolve("{&x > 1 ? yes : no} / {&loot toa}",
			false));
		assertEquals("{kc toa}", VariableResolver.unprefixed("{^kc toa}"));
	}

	@Test
	public void theDateIsWrittenAsGroundMarkerVariablesWritesIt()
	{
		assertEquals("October 1st 2026", VariableResolver.date(LocalDate.of(2026, 10, 1)));
		assertEquals("October 12th 2026", VariableResolver.date(LocalDate.of(2026, 10, 12)));
		assertEquals("October 23rd 2026", VariableResolver.date(LocalDate.of(2026, 10, 23)));
		assertEquals(Skill.AGILITY, VariableResolver.skill("Agility"));
		assertNull(VariableResolver.skill("sailingg"));
	}
}
