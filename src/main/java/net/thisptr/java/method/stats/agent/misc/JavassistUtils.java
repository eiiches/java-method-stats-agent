package net.thisptr.java.method.stats.agent.misc;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import net.thisptr.java.method.stats.agent.cofig.MethodName;

public class JavassistUtils {

	public static class MethodNotFoundException extends RuntimeException {
		private static final long serialVersionUID = 2182870594924116925L;

		public MethodNotFoundException() {
			super();
		}

		public MethodNotFoundException(Throwable cause) {
			super(cause);
		}
	}

	public static class AmbiguousMethodNameException extends RuntimeException {
		private static final long serialVersionUID = 6743054262921148756L;
	}

	public static CtMethod findMethod(final CtClass clazz, final MethodName name) {
		final CtMethod[] methods;
		try {
			methods = clazz.getDeclaredMethods(name.methodName);
		} catch (NotFoundException e) {
			throw new MethodNotFoundException(e);
		}
		if (methods.length == 0)
			throw new MethodNotFoundException();

		if (name.parameterClassNames == null) {
			if (methods.length > 1)
				throw new AmbiguousMethodNameException();
			return methods[0];
		} else {
			method: for (final CtMethod method : methods) {
				try {
					final CtClass[] types = method.getParameterTypes();
					if (name.parameterClassNames.size() != types.length)
						continue;
					for (int i = 0; i < name.parameterClassNames.size(); ++i) {
						if (!name.parameterClassNames.get(i).equals(types[i].getName()))
							continue method;
					}
					return method;
				} catch (NotFoundException e) {
					continue;
				}
			}
			throw new MethodNotFoundException();
		}
	}
}
