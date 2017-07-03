package net.thisptr.java.method.stats.agent.templates;

import net.thisptr.java.method.stats.agent.mbeans.JmxTimer;

public class TimerCodeTemplate implements CodeTemplate {
	@Override
	public Class<?> clazz() {
		return JmxTimer.class;
	}

	@Override
	public String create(final String instance) {
		return String.format("(%s).update(System.nanoTime() - $time0, java.util.concurrent.TimeUnit.NANOSECONDS);", instance);
	}
}