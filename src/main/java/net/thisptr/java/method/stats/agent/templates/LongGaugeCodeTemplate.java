package net.thisptr.java.method.stats.agent.templates;

import net.thisptr.java.method.stats.agent.mbeans.JmxLongGauge;

public class LongGaugeCodeTemplate implements CodeTemplate {
	private final String expr;

	public LongGaugeCodeTemplate(final String expr) {
		this.expr = expr;
	}

	@Override
	public Class<?> clazz() {
		return JmxLongGauge.class;
	}

	@Override
	public String create(final String instance) {
		return String.format("(%s).set(%s);", instance, expr != null ? expr : "0L");
	}
}