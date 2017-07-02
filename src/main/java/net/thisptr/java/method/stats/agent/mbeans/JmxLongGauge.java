package net.thisptr.java.method.stats.agent.mbeans;

import java.util.concurrent.atomic.AtomicLong;

public class JmxLongGauge implements JmxLongGaugeMXBean {
	private final AtomicLong value;

	public JmxLongGauge() {
		this.value = new AtomicLong();
	}

	@Override
	public long getValue() {
		return value.get();
	}

	public void set(final long value) {
		this.value.lazySet(value);
	}

	public void set(final int value) {
		set((long) value);
	}
}