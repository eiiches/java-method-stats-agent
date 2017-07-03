package net.thisptr.java.method.stats.agent.templates;

import net.thisptr.java.method.stats.agent.mbeans.JmxDoubleGauge;

public class DoubleGaugeCodeTemplate implements CodeTemplate {
	private final String expr;

	public DoubleGaugeCodeTemplate(final String expr) {
		this.expr = expr;
	}

	@Override
	public Class<?> clazz() {
		return JmxDoubleGauge.class;
	}

	@Override
	public String create(final String instance) {
		return String.format("(%s).set(%s);", instance, expr != null ? expr : "0.0");
	}
}