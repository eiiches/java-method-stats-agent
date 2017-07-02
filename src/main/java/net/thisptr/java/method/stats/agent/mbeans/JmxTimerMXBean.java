package net.thisptr.java.method.stats.agent.mbeans;

import javax.management.MXBean;

@MXBean
public interface JmxTimerMXBean extends JmxMeterMXBean {
	JmxSnapshot getSnapshot();
}