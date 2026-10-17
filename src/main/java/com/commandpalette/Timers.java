package com.commandpalette;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;

/**
 * Timers started from the palette, each of which sends a notification and a chat message when it runs out. They last only as long as
 * the client is open.
 */
@Singleton
class Timers
{
	/**
	 * How long a timer is for, and what it is for.
	 */
	static final class Request
	{
		private final double seconds;
		private final String label;

		private Request(double seconds, String label)
		{
			this.seconds = seconds;
			this.label = label;
		}

		double getSeconds()
		{
			return seconds;
		}

		String getLabel()
		{
			return label;
		}

		/**
		 * "5m timer: herb run"
		 */
		String describe()
		{
			return Durations.format(seconds) + " timer" + (label.isEmpty() ? "" : ": " + label);
		}
	}

	/**
	 * A timer that is running, as it stood when asked for.
	 */
	static final class Status
	{
		private final Request request;
		private final long secondsLeft;
		private final Runnable cancel;

		private Status(Request request, long secondsLeft, Runnable cancel)
		{
			this.request = request;
			this.secondsLeft = secondsLeft;
			this.cancel = cancel;
		}

		Request getRequest()
		{
			return request;
		}

		long getSecondsLeft()
		{
			return secondsLeft;
		}

		void cancel()
		{
			cancel.run();
		}
	}

	private static final class Running
	{
		private final Request request;
		private final long endsAt;
		private ScheduledFuture<?> future;

		private Running(Request request, long endsAt)
		{
			this.request = request;
			this.endsAt = endsAt;
		}
	}

	private static final double MAX_SECONDS = 24 * 60 * 60;
	// "1 hour 30 minutes 15 seconds"
	private static final int MAX_DURATION_WORDS = 6;

	private final ScheduledExecutorService executor;
	private final Notifier notifier;
	private final Client client;
	private final ClientThread clientThread;
	// started and cancelled on the Swing thread, finished on the executor's
	private final List<Running> running = new CopyOnWriteArrayList<>();

	@Inject
	Timers(ScheduledExecutorService executor, Notifier notifier, Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.executor = executor;
		this.notifier = notifier;
	}

	/**
	 * @param text a length of time and then, optionally, what the timer is for: "5m herb run", "1h 30m", "90s".
	 *             A number on its own is in minutes.
	 * @return null if the text doesn't start with a length of time, or it is none at all or over a day
	 */
	static Request parse(String text)
	{
		final String[] words = text.trim().split("\\s+");
		for (int count = Math.min(words.length, MAX_DURATION_WORDS); count > 0; count--)
		{
			double seconds = Durations.parse(String.join(" ", List.of(words).subList(0, count)));
			if (seconds < 0 && count == 1 && words[0].matches("\\d+(\\.\\d+)?"))
			{
				seconds = Double.parseDouble(words[0]) * 60;
			}
			if (seconds >= 0)
			{
				final String label = String.join(" ", List.of(words).subList(count, words.length));
				return seconds >= 1 && seconds <= MAX_SECONDS ? new Request(seconds, label) : null;
			}
		}
		return null;
	}

	void start(Request request)
	{
		final long millis = Math.round(request.getSeconds() * 1000);
		final Running timer = new Running(request, System.currentTimeMillis() + millis);
		running.add(timer);
		timer.future = executor.schedule(() ->
		{
			// not if it was cancelled just as it ran out
			if (running.remove(timer))
			{
				final String message = request.getLabel().isEmpty()
					? "Your " + Durations.format(request.getSeconds()) + " timer is done"
					: "Timer done: " + request.getLabel();
				// RuneLite's notifications are, as they come, only sent while the client isn't being used, so
				// the chat is told too
				notifier.notify(message);
				clientThread.invoke(() ->
				{
					if (client.getGameState() == GameState.LOGGED_IN)
					{
						client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
					}
				});
			}
		}, millis, TimeUnit.MILLISECONDS);
	}

	/**
	 * A command to cancel each timer that is running, the one that ends soonest first.
	 */
	void collect(List<Command> out)
	{
		for (Status timer : running())
		{
			out.add(new Command("Cancel " + timer.getRequest().describe(),
				Durations.format(timer.getSecondsLeft()) + " left", Icons.TIMER, timer::cancel));
		}
	}

	/**
	 * The timers that are running, the one that ends soonest first.
	 */
	List<Status> running()
	{
		final long now = System.currentTimeMillis();
		return running.stream()
			.sorted((a, b) -> Long.compare(a.endsAt, b.endsAt))
			.map(timer -> new Status(timer.request, Math.max(1, Math.round((timer.endsAt - now) / 1000.0)),
				() -> cancel(timer)))
			.collect(Collectors.toList());
	}

	private void cancel(Running timer)
	{
		if (running.remove(timer))
		{
			timer.future.cancel(false);
		}
	}

	void reset()
	{
		for (Running timer : running)
		{
			cancel(timer);
		}
	}
}
