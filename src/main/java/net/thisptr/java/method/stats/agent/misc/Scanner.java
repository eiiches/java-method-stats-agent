package net.thisptr.java.method.stats.agent.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scanner {
	private static Pattern WHITE_SPACE_PATTERN = Pattern.compile("^\\s+");

	private final String s;
	private final Matcher m;

	public Scanner(final String s) {
		this.s = s;
		this.m = WHITE_SPACE_PATTERN.matcher(s);
	}

	public void skipWhitespaces() {
		m.usePattern(WHITE_SPACE_PATTERN);
		if (m.find())
			m.region(m.end(), m.regionEnd());
	}

	public void advance() {
		m.region(m.end(), m.regionEnd());
	}

	public void advanceAndSkipWhitespaces() {
		advance();
		skipWhitespaces();
	}

	public String group(final String name) {
		return m.group(name);
	}

	public boolean match(final Pattern p) {
		m.usePattern(p);
		return m.find();
	}

	public void matchOrThrow(final Pattern p) {
		m.usePattern(p);
		if (!m.find())
			throw new IllegalArgumentException("Expecting " + p.toString() + ", but got: " + trailingCharacters());
	}

	public boolean hitEnd() {
		return m.hitEnd();
	}

	public String trailingCharacters() {
		return s.substring(m.regionStart());
	}
}