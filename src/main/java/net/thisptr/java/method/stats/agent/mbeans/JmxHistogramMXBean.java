package net.thisptr.java.method.stats.agent.mbeans;

import javax.management.MXBean;

@MXBean
public interface JmxHistogramMXBean extends JmxCounterMXBean {
	JmxSnapshot getSnapshot();
}