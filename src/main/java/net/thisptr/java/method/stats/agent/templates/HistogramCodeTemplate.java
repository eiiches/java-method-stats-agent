package net.thisptr.java.method.stats.agent.templates;

import net.thisptr.java.method.stats.agent.mbeans.JmxHistogram;

public class HistogramCodeTemplate implements CodeTemplate {
	private final String expr;

	public HistogramCodeTemplate(final String expr) {
		this.expr = expr;
	}

	@Override
	public Class<?> clazz() {
		return JmxHistogram.class;
	}

	@Override
	public String create(final String instance) {
		return String.format("(%s).update(%s);", instance, expr != null ? expr : "1L");
	}
}