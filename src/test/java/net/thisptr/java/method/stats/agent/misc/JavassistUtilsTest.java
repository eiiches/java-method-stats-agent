package net.thisptr.java.method.stats.agent.misc;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import net.thisptr.java.method.stats.agent.cofig.MethodName;
import net.thisptr.java.method.stats.agent.misc.JavassistUtils.AmbiguousMethodNameException;
import net.thisptr.java.method.stats.agent.misc.JavassistUtils.MethodNotFoundException;

public class JavassistUtilsTest {
	public static class Class1 {
		public int method1(int i) {
			return 0;
		}

		public boolean method1(int i, int j) {
			return false;
		}

		public long method1(Integer i) {
			return 0;
		}

		public byte method2(int i) {
			return '\0';
		}
	}

	private CtClass class1;

	@Before
	public void setup() throws NotFoundException {
		final ClassPool cp = new ClassPool(true);
		this.class1 = cp.get("net.thisptr.java.method.stats.agent.misc.JavassistUtilsTest$Class1");
	}

	@Test
	public void testFindMethodWithSignature() throws Exception {
		final CtMethod method1i = JavassistUtils.findMethod(class1, MethodName.valueOf("net.thisptr.java.method.stats.agent.misc.JavassistUtilsTest$Class1.method1(int)"));
		assertEquals("int", method1i.getReturnType().getName());

		final CtMethod method1l = JavassistUtils.findMethod(class1, MethodName.valueOf("net.thisptr.java.method.stats.agent.misc.JavassistUtilsTest$Class1.method1(java.lang.Integer)"));
		assertEquals("long", method1l.getReturnType().getName());

		final CtMethod method1b = JavassistUtils.findMethod(class1, MethodName.valueOf("net.thisptr.java.method.stats.agent.misc.JavassistUtilsTest$Class1.method1(int, int)"));
		assertEquals("boolean", method1b.getReturnType().getName());
	}

	@Test
	public void testFindMethodWithoutSignature() throws Exception {
		final CtMethod method1b = JavassistUtils.findMethod(class1, MethodName.valueOf("net.thisptr.java.method.stats.agent.misc.JavassistUtilsTest$Class1.method2"));
		assertEquals("byte", method1b.getReturnType().getName());
	}

	@Test(expected = MethodNotFoundException.class)
	public void testFindMethodNoMatchingName() throws Exception {
		JavassistUtils.findMethod(class1, MethodName.valueOf("net.thisptr.java.method.stats.agent.misc.JavassistUtilsTest$Class1.missingMethod"));
	}

	@Test(expected = MethodNotFoundException.class)
	public void testFindMethodNoMatchingArgs() throws Exception {
		JavassistUtils.findMethod(class1, MethodName.valueOf("net.thisptr.java.method.stats.agent.misc.JavassistUtilsTest$Class1.method1(long)"));
	}

	@Test(expected = AmbiguousMethodNameException.class)
	public void testFindMethodAmbiguous() throws Exception {
		JavassistUtils.findMethod(class1, MethodName.valueOf("net.thisptr.java.method.stats.agent.misc.JavassistUtilsTest$Class1.method1"));
	}
}
