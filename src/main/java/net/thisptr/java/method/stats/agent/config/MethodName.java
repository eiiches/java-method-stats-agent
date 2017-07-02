package net.thisptr.java.method.stats.agent.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonValue;

import net.thisptr.java.method.stats.agent.misc.Scanner;

public class MethodName {
	public final String className;
	public final String methodName;
	public final List<String> parameterClassNames;

	public MethodName(final String className, final String methodName) {
		this(className, methodName, null);
	}

	public MethodName(final String className, final String methodName, final List<String> parameterClassNames) {
		this.className = className;
		this.methodName = methodName;
		this.parameterClassNames = parameterClassNames;
	}

	private static Pattern CLASS_NAME_FRAGMENT = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*\\s*(\\.\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*)*(\\$\\s*[a-zA-Z0-9_]+\\s*)*");
	private static Pattern METHOD_NAME_FRAGMENT = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

	private static Pattern CLASS_NAME_AND_METHOD_NAME_PATTERN = Pattern.compile("^"
			+ "(?<class>" + CLASS_NAME_FRAGMENT + ")"
			+ "\\."
			+ "(?<method>" + METHOD_NAME_FRAGMENT + ")"
			+ "");

	private static Pattern CLASS_NAME_PATTERN = Pattern.compile("^"
			+ "(?<class>" + CLASS_NAME_FRAGMENT + ")"
			+ "");

	private static Pattern PAREN_LEFT_PATTERN = Pattern.compile("^\\(");
	private static Pattern PAREN_RIGHT_PATTERN = Pattern.compile("^\\)");
	private static Pattern COMMA_PATTERN = Pattern.compile("^,");

	@JsonCreator(mode = Mode.DELEGATING)
	public static MethodName valueOf(final String name) {
		final Scanner s = new Scanner(name);
		s.skipWhitespaces();

		s.matchOrThrow(CLASS_NAME_AND_METHOD_NAME_PATTERN);
		final String className = s.group("class").replaceAll("\\s+", "");
		final String methodName = s.group("method").replaceAll("\\s+", "");
		s.advanceAndSkipWhitespaces();

		if (s.hitEnd())
			return new MethodName(className, methodName);

		s.matchOrThrow(PAREN_LEFT_PATTERN);
		s.advanceAndSkipWhitespaces();

		final List<String> parameterClassNames = new ArrayList<>();

		boolean first = true;
		while (true) {
			if (s.match(PAREN_RIGHT_PATTERN)) {
				s.advanceAndSkipWhitespaces();
				break;
			}

			if (!first) {
				s.matchOrThrow(COMMA_PATTERN);
				s.advanceAndSkipWhitespaces();
			}

			s.matchOrThrow(CLASS_NAME_PATTERN);
			parameterClassNames.add(s.group("class").replaceAll("\\s+", ""));
			s.advanceAndSkipWhitespaces();
			first = false;
		}

		if (s.hitEnd())
			return new MethodName(className, methodName, parameterClassNames);

		throw new IllegalArgumentException("trailing characters: " + s.trailingCharacters());
	}

	@Override
	@JsonValue
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(className);
		builder.append('.');
		builder.append(methodName);
		if (parameterClassNames != null) {
			builder.append('(');
			String sep = "";
			for (final String parameterClassName : parameterClassNames) {
				builder.append(sep);
				builder.append(parameterClassName);
				sep = ", ";
			}
			builder.append(')');
		}
		return builder.toString();
	}
}
