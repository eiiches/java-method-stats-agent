package net.thisptr.java.method.stats.agent.templates;

import net.thisptr.java.method.stats.agent.mbeans.JmxCounter;

public class CounterCodeTemplate implements CodeTemplate {
	private final String expr;

	public CounterCodeTemplate(final String expr) {
		this.expr = expr;
	}

	@Override
	public Class<?> clazz() {
		return JmxCounter.class;
	}

	@Override
	public String create(final String instance) {
		return String.format("(%s).inc(%s);", instance, expr != null ? expr : "1L");
	}
}