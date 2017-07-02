package net.thisptr.java.method.stats.agent.misc;

import java.util.List;

public class Java {
	public static String StringLiteral(final String s) {
		return "\"" + s.replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"") + "\"";
	}

	public static String NewArrayWithInitializer(final String type, final List<String> items) {
		final StringBuilder builder = new StringBuilder("new ");
		builder.append(type);
		builder.append("[]");
		builder.append("{");
		String sep = "";
		for (final String item : items) {
			builder.append(sep);
			builder.append(item);
			sep = ", ";
		}
		builder.append("}");
		return builder.toString();
	}

	public static String FunctionCall(final String target, final String method, final List<String> args) {
		return FunctionCall(target + "." + method, args);
	}

	public static String FunctionCall(final String function, final List<String> args) {
		final StringBuilder builder = new StringBuilder(function);
		builder.append("(");
		String sep = "";
		for (final String arg : args) {
			builder.append(sep);
			builder.append(arg);
			sep = ", ";
		}
		builder.append(")");
		return builder.toString();
	}

	public static String Cast(final String type, final String expr) {
		return "((" + type + ")" + expr + ")";
	}

	public static String FieldAccess(final String target, final String member) {
		return target + "." + member;
	}

	public static String Assignment(final String target, final String expr) {
		return target + " = " + expr;
	}

	public static String BinaryOperator(final String lhs, final String op, final String rhs) {
		return lhs + op + rhs;
	}

	public static String Null() {
		return "null";
	}

	public static String IfStatement(final String condition, final String statement) {
		return "if (" + condition + ")" + statement;
	}

	public static String Statement(final String expr) {
		return expr + ";";
	}
}