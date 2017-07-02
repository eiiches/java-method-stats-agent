package net.thisptr.java.method.stats.agent.mbeans;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Timer;

public class JmxTimer implements JmxTimerMXBean {
	private final Timer timer;

	public JmxTimer() {
		this.timer = new Timer();
	}

	@Override
	public long getCount() {
		return timer.getCount();
	}

	@Override
	public double getFifteenMinuteRate() {
		return timer.getFifteenMinuteRate();
	}

	@Override
	public double getFiveMinuteRate() {
		return timer.getFiveMinuteRate();
	}

	@Override
	public double getMeanRate() {
		return timer.getMeanRate();
	}

	@Override
	public double getOneMinuteRate() {
		return timer.getOneMinuteRate();
	}

	@Override
	public JmxSnapshot getSnapshot() {
		return new JmxSnapshot(timer.getSnapshot());
	}

	public void update(long duration, TimeUnit unit) {
		timer.update(duration, unit);
	}
}
