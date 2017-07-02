package net.thisptr.java.method.stats.agent.mbeans;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;

public class JmxHistogram implements JmxHistogramMXBean {
	private final Histogram histogram;

	public JmxHistogram() {
		this.histogram = new Histogram(new ExponentiallyDecayingReservoir());
	}

	@Override
	public long getCount() {
		return histogram.getCount();
	}

	@Override
	public JmxSnapshot getSnapshot() {
		return new JmxSnapshot(histogram.getSnapshot());
	}

	public void update(int value) {
		histogram.update(value);
	}

	public void update(long value) {
		histogram.update(value);
	}
}
