package com.commandpalette;

import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class AnswersTest
{
	private final Answers answers = new Answers();
	private int changes;

	private List<String> texts()
	{
		return answers.list().stream().map(Answers.Answer::getText).collect(Collectors.toList());
	}

	@Test
	public void theLatestComesFirstAndAskingAgainMovesItUp()
	{
		answers.setOnChange(() -> changes++);
		answers.add("a", null);
		answers.add("b", null);
		answers.add("a", null);
		assertEquals(List.of("a", "b"), texts());
		assertEquals(3, changes);
	}

	@Test
	public void onlyTheLastFewAreKept()
	{
		for (int i = 0; i < Answers.MAX + 5; i++)
		{
			answers.add("answer " + i, null);
		}
		assertEquals(Answers.MAX, answers.list().size());
		assertEquals("answer " + (Answers.MAX + 4), texts().get(0));
	}

	@Test
	public void anAnswerCanCopySomethingPlainerThanItShows()
	{
		answers.add("35m / 4.2k = 8,333.33", "8333.33", null);
		assertEquals("8333.33", answers.list().get(0).getCopy());
		answers.clear();
		assertEquals(List.of(), texts());
	}
}
