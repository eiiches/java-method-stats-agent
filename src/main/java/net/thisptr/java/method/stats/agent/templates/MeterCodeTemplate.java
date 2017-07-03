package net.thisptr.java.method.stats.agent.templates;

import net.thisptr.java.method.stats.agent.mbeans.JmxMeter;

public class MeterCodeTemplate implements CodeTemplate {
	private final String expr;

	public MeterCodeTemplate(final String expr) {
		this.expr = expr;
	}

	@Override
	public Class<?> clazz() {
		return JmxMeter.class;
	}

	@Override
	public String create(final String instance) {
		return String.format("(%s).mark(%s);", instance, expr != null ? expr : "1L");
	}
}