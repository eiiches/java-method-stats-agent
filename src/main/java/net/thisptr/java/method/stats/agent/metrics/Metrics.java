package net.thisptr.java.method.stats.agent.metrics;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class Metrics {

	public static ConcurrentMap<ObjectName, Object> METRICS = new ConcurrentHashMap<>();

	public static Object getMetrics(final Class<?> metricsClass, final String domain, final String[] keyvalues) {
		final Hashtable<String, String> props = new Hashtable<String, String>();
		for (int i = 0; i < keyvalues.length; i += 2)
			props.put(keyvalues[i], keyvalues[i + 1]);
		try {
			final ObjectName on = new ObjectName(domain, props);
	
			Object o = METRICS.get(on);
			if (o != null)
				return o;
	
			o = metricsClass.newInstance();
			Object po = METRICS.putIfAbsent(on, o);
			if (po != null)
				return po;
	
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			try {
				mbs.registerMBean(o, on);
			} catch (Exception e) {
				e.printStackTrace();
				return o;
			}
	
			return o;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
