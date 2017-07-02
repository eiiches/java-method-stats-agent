package net.thisptr.java.method.stats.agent.misc;

import java.util.regex.Pattern;

import javax.management.ObjectName;

public class ObjectNameUtils {
	private static final Pattern RESERVED = Pattern.compile("[*?:=,\"]");

	/**
	 * https://docs.oracle.com/javase/7/docs/api/javax/management/ObjectName.html
	 * 
	 * @param value
	 * @return
	 */
	public static String sanitize(final String value) {
		return RESERVED.matcher(value).replaceAll("_");
	}

	public static String quote(final String value) {
		return ObjectName.quote(value);
	}
}
