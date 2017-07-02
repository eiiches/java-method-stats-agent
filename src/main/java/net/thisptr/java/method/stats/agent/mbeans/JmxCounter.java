package net.thisptr.java.method.stats.agent.mbeans;

import com.codahale.metrics.Counter;

public class JmxCounter implements JmxCounterMXBean {
	private final Counter counter;

	public JmxCounter() {
		this.counter = new Counter();
	}

	@Override
	public long getCount() {
		return counter.getCount();
	}

	public void inc() {
		counter.inc();
	}

	public void inc(long n) {
		counter.inc(n);
	}

	public void dec() {
		counter.dec();
	}

	public void dec(long n) {
		counter.dec(n);
	}
}