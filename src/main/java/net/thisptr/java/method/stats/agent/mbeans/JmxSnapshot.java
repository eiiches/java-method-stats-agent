package net.thisptr.java.method.stats.agent.mbeans;

import com.codahale.metrics.Snapshot;

public class JmxSnapshot {
	private Snapshot snapshot;

	public JmxSnapshot(final Snapshot snapshot) {
		this.snapshot = snapshot;
	}

	public double get50th() {
		return snapshot.getMedian();
	}

	public double get75th() {
		return snapshot.get75thPercentile();
	}

	public double get95th() {
		return snapshot.get95thPercentile();
	}

	public double get98th() {
		return snapshot.get98thPercentile();
	}

	public double get99th() {
		return snapshot.get99thPercentile();
	}

	public double get999th() {
		return snapshot.get999thPercentile();
	}

	public double getMax() {
		return snapshot.getMax();
	}

	public double getMean() {
		return snapshot.getMean();
	}

	public double getMin() {
		return snapshot.getMin();
	}

	public double getStddev() {
		return snapshot.getStdDev();
	}
}