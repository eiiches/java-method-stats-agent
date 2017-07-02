package net.thisptr.java.method.stats.agent.misc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ObjectNameUtilsTest {
	@Test
	public void testSanitize() throws Exception {
		assertEquals("hbase_namespace", ObjectNameUtils.sanitize("hbase:namespace"));
	}

	@Test
	public void testQuote() throws Exception {
		assertEquals("\"hbase:namespace\"", ObjectNameUtils.quote("hbase:namespace"));
	}
}
