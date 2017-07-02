package net.thisptr.java.method.stats.agent.mbeans;

import javax.management.MXBean;

@MXBean
public interface JmxMeterMXBean extends JmxCounterMXBean {
	double getMeanRate();

	double getOneMinuteRate();

	double getFiveMinuteRate();

	double getFifteenMinuteRate();
}