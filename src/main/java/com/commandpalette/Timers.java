package com.commandpalette;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.Notifier;

/**
 * Timers started from the palette, each of which sends a notification when it runs out. They last only as long as
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
	// started and cancelled on the Swing thread, finished on the executor's
	private final List<Running> running = new CopyOnWriteArrayList<>();

	@Inject
	Timers(ScheduledExecutorService executor, Notifier notifier)
	{
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
				notifier.notify(request.getLabel().isEmpty()
					? "Your " + Durations.format(request.getSeconds()) + " timer is done"
					: "Timer done: " + request.getLabel());
			}
		}, millis, TimeUnit.MILLISECONDS);
	}

	/**
	 * A command to cancel each timer that is running, the one that ends soonest first.
	 */
	void collect(List<Command> out)
	{
		final long now = System.currentTimeMillis();
		running.stream()
			.sorted((a, b) -> Long.compare(a.endsAt, b.endsAt))
			.forEach(timer -> out.add(new Command("Cancel " + timer.request.describe(),
				Durations.format(Math.max(1, Math.round((timer.endsAt - now) / 1000.0))) + " left", Icons.TIMER,
				() -> cancel(timer))));
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
