package net.thisptr.java.method.stats.agent.mbeans;

import java.util.concurrent.atomic.AtomicLong;

public class JmxDoubleGauge implements JmxDoubleGaugeMXBean {
	private final AtomicLong value;

	public JmxDoubleGauge() {
		this.value = new AtomicLong();
	}

	@Override
	public double getValue() {
		return Double.longBitsToDouble(value.get());
	}

	public void set(final double value) {
		this.value.lazySet(Double.doubleToLongBits(value));
	}

	public void set(final float value) {
		set((double) value);
	}
}