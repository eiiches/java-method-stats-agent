package net.thisptr.java.method.stats.agent.mbeans;

import com.codahale.metrics.Meter;

public class JmxMeter implements JmxMeterMXBean {
	private final Meter meter;

	public JmxMeter() {
		this.meter = new Meter();
	}

	@Override
	public long getCount() {
		return meter.getCount();
	}

	@Override
	public double getFifteenMinuteRate() {
		return meter.getFifteenMinuteRate();
	}

	@Override
	public double getFiveMinuteRate() {
		return meter.getFiveMinuteRate();
	}

	@Override
	public double getMeanRate() {
		return meter.getMeanRate();
	}

	@Override
	public double getOneMinuteRate() {
		return meter.getOneMinuteRate();
	}

	public void mark() {
		meter.mark();
	}

	public void mark(long n) {
		meter.mark(n);
	}
}