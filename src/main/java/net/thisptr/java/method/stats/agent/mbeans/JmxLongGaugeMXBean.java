package net.thisptr.java.method.stats.agent.mbeans;

import javax.management.MXBean;

@MXBean
public interface JmxLongGaugeMXBean {
	long getValue();
}