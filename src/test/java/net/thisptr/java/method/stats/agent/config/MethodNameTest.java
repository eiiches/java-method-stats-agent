package net.thisptr.java.method.stats.agent.config;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import net.thisptr.java.method.stats.agent.config.MethodName;

public class MethodNameTest {
	@Test
	public void testNoParams() {
		final MethodName name = MethodName.valueOf("package. OuterClass$InnerClass$ 1.invoke");
		assertEquals("package.OuterClass$InnerClass$1", name.className);
		assertEquals("invoke", name.methodName);
		assertEquals(null, name.parameterClassNames);
		assertEquals("package.OuterClass$InnerClass$1.invoke", name.toString());
	}

	@Test
	public void testParams() {
		final MethodName name = MethodName.valueOf("package. OuterClass$InnerClass$ 1.invoke ( java.lang.String, java.lang.Long, int)");
		assertEquals("package.OuterClass$InnerClass$1", name.className);
		assertEquals("invoke", name.methodName);
		assertEquals(Arrays.asList(new String[] {
				"java.lang.String",
				"java.lang.Long",
				"int"
		}), name.parameterClassNames);
		assertEquals("package.OuterClass$InnerClass$1.invoke(java.lang.String, java.lang.Long, int)", name.toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParamsWithTrailingChars() {
		MethodName.valueOf("package. OuterClass$InnerClass$ 1.invoke ( java.lang.String, java.lang.Long, int)foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoParamsWithTrailingChars() {
		MethodName.valueOf("package. OuterClass$InnerClass$ 1.invoke foo");
	}
}
