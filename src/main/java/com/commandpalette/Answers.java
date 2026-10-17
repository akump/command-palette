package com.commandpalette;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import javax.swing.Icon;

/**
 * The answers worked out from the palette, a sum, a tax or an item's price, kept for the side panel's Answers tab
 * so they aren't lost to the chat. They last only as long as the client is open.
 */
@Singleton
class Answers
{
	static final int MAX = 15;

	/**
	 * One answer, and the text that copying it puts on the clipboard.
	 */
	static final class Answer
	{
		private final String text;
		private final String copy;
		private final Icon icon;

		Answer(String text, String copy, Icon icon)
		{
			this.text = text;
			this.copy = copy;
			this.icon = icon;
		}

		String getText()
		{
			return text;
		}

		String getCopy()
		{
			return copy;
		}

		Icon getIcon()
		{
			return icon;
		}
	}

	// added to from the client thread as well as the Swing one
	private final List<Answer> answers = new ArrayList<>();
	private volatile Runnable onChange = () -> {};

	/**
	 * @param onChange run, on any thread, after an answer is added or they are cleared
	 */
	void setOnChange(Runnable onChange)
	{
		this.onChange = onChange;
	}

	void add(String text, Icon icon)
	{
		add(text, text, icon);
	}

	void add(String text, String copy, Icon icon)
	{
		synchronized (answers)
		{
			// asking again moves an answer to the top rather than listing it twice
			answers.removeIf(answer -> answer.getText().equals(text));
			answers.add(0, new Answer(text, copy, icon));
			if (answers.size() > MAX)
			{
				answers.remove(MAX);
			}
		}
		onChange.run();
	}

	/**
	 * Latest first.
	 */
	List<Answer> list()
	{
		synchronized (answers)
		{
			return new ArrayList<>(answers);
		}
	}

	void clear()
	{
		synchronized (answers)
		{
			answers.clear();
		}
		onChange.run();
	}
}
