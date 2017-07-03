package net.thisptr.java.method.stats.agent.templates;

public interface CodeTemplate {
	Class<?> clazz();

	String create(String instance);
}