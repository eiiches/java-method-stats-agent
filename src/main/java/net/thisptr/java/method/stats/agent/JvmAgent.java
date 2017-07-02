package net.thisptr.java.method.stats.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import net.thisptr.java.method.stats.agent.cofig.JvmAgentConfig;
import net.thisptr.java.method.stats.agent.cofig.JvmAgentConfig.InstrumentConfig;
import net.thisptr.java.method.stats.agent.cofig.JvmAgentConfig.InstrumentConfig.CacheScope;
import net.thisptr.java.method.stats.agent.cofig.JvmAgentConfig.InstrumentConfig.InstrumentType;
import net.thisptr.java.method.stats.agent.cofig.JvmAgentConfig.InstrumentConfig.JmxObjectName;
import net.thisptr.java.method.stats.agent.mbeans.JmxCounter;
import net.thisptr.java.method.stats.agent.mbeans.JmxDoubleGauge;
import net.thisptr.java.method.stats.agent.mbeans.JmxHistogram;
import net.thisptr.java.method.stats.agent.mbeans.JmxLongGauge;
import net.thisptr.java.method.stats.agent.mbeans.JmxMeter;
import net.thisptr.java.method.stats.agent.mbeans.JmxTimer;
import net.thisptr.java.method.stats.agent.metrics.Metrics;
import net.thisptr.java.method.stats.agent.misc.Java;
import net.thisptr.java.method.stats.agent.misc.JavassistUtils;
import net.thisptr.java.method.stats.agent.misc.ObjectNameUtils;

public class JvmAgent {

	public static void premain(final String args, final Instrumentation inst) throws Exception {
		final JvmAgentConfig config = JvmAgentConfig.fromString(args);

		final Map<String, List<InstrumentConfig>> instruments = new HashMap<>();
		for (final InstrumentConfig instrument : config.instruments)
			instruments.computeIfAbsent(instrument.method.className, $ -> new ArrayList<>()).add(instrument);

		final ClassPool cp = createClassPool(config);
		for (final String className : instruments.keySet()) {
			try {
				final CtClass clazz = cp.get(className.replace('.', '/'));
				if (clazz == null)
					throw new NotFoundException(className);
			} catch (Exception e) {
				System.err.println("Cannot find class: " + className); // FIXME
			}
		}

		inst.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException {
				final List<InstrumentConfig> methodInstruments = instruments.get(className.replace('/', '.'));
				if (methodInstruments == null)
					return null;

				try {
					final ClassPool cp = createClassPool(config);
					final CtClass clazz = cp.get(className.replace('/', '.'));

					for (final InstrumentConfig instrument : methodInstruments) {
						try {
							instrumentMethod(cp, clazz, instrument);
						} catch (final Exception e) {
							e.printStackTrace(); // FIXME
						}
					}

					final byte[] bytes = clazz.toBytecode();
					clazz.detach();
					return bytes;
				} catch (Exception e) {
					e.printStackTrace(); // FIXME
					return null;
				}
			}
		});
	}

	private static String constructFieldAccess(final CtField field) {
		if ((field.getModifiers() & Modifier.STATIC) > 0) {
			return Java.FieldAccess(field.getDeclaringClass().getName(), field.getName());
		} else {
			return Java.FieldAccess("$0", field.getName());
		}
	}

	private static String constructGetMetricsExpression(final JmxObjectName name, final Class<?> metricsClass) {
		final List<String> keyvalues = new ArrayList<>();
		final List<String> keyvalueExprs = new ArrayList<>();
		final AtomicBoolean dynamic = new AtomicBoolean(false);
		name.keys.forEach((k, v) -> {
			keyvalues.add(k);
			keyvalueExprs.add(Java.StringLiteral(k));
			if (v.startsWith("${") && v.endsWith("}")) {
				dynamic.set(true);
				final String expr = v.substring(2, v.length() - 1);
				keyvalueExprs.add(expr.replaceAll(Pattern.quote("$jmx."), ObjectNameUtils.class.getName() + "."));
			} else {
				keyvalues.add(v);
				keyvalueExprs.add(Java.StringLiteral(v));
			}
		});
		if (!dynamic.get()) {
			// if name is static, pre-register the mxbean.
			Metrics.getMetrics(metricsClass, name.domain, keyvalues.toArray(new String[keyvalues.size()]));
		}
		return Java.Cast(metricsClass.getName(), Java.FunctionCall(Metrics.class.getName(), "getMetrics", Arrays.asList(new String[] {
				metricsClass.getName() + ".class",
				Java.StringLiteral(name.domain),
				Java.NewArrayWithInitializer("String", keyvalueExprs),
		})));
	}

	public static void instrumentMethod(final ClassPool cp, final CtClass clazz, final InstrumentConfig instrument) throws CannotCompileException, NotFoundException {
		final CtMethod method = JavassistUtils.findMethod(clazz, instrument.method);
		if (method == null) {
			System.err.println("Cannot find method: " + instrument.method);
			return;
		}
		System.err.println(instrument.method);

		CtField cacheField = null;
		Class<?> metricsClass = null;
		if (instrument.cacheScope != null) {

			switch (instrument.type) {
				case COUNTER:
					metricsClass = JmxCounter.class;
					break;
				case HISTOGRAM:
					metricsClass = JmxHistogram.class;
					break;
				case METER:
					metricsClass = JmxMeter.class;
					break;
				case TIMER:
					metricsClass = JmxTimer.class;
					break;
				case GAUGE_LONG:
					metricsClass = JmxLongGauge.class;
					break;
				case GAUGE_DOUBLE:
					metricsClass = JmxDoubleGauge.class;
					break;
				default:
					break;
			}

			if (metricsClass != null) {
				final String fieldName = "_m_" + System.identityHashCode(instrument);
				final CtField field = new CtField(cp.get(metricsClass.getName()), fieldName, clazz);
				if (instrument.cacheScope == CacheScope.CLASS)
					field.setModifiers(Modifier.STATIC);
				clazz.addField(field);
				cacheField = field;
			}
		}

		final String errorFieldName = "_e_" + System.identityHashCode(instrument);
		final CtField errorField = new CtField(cp.get("boolean"), errorFieldName, clazz);
		errorField.setModifiers(Modifier.STATIC);
		clazz.addField(errorField);

		String methodName = null;
		List<String> methodArgs = null;
		switch (instrument.type) {
			case COUNTER:
				methodName = "inc";
				methodArgs = instrument.value != null
						? Arrays.asList(instrument.value)
						: Arrays.asList("1L");
				break;
			case HISTOGRAM:
				methodName = "update";
				methodArgs = instrument.value != null
						? Arrays.asList(instrument.value)
						: Arrays.asList("1L");
				break;
			case METER:
				methodName = "mark";
				methodArgs = instrument.value != null
						? Arrays.asList(instrument.value)
						: Arrays.asList("1L");
				break;
			case TIMER:
				methodName = "update";
				methodArgs = Arrays.asList("System.nanoTime() - $time0", "java.util.concurrent.TimeUnit.NANOSECONDS");
				break;
			case GAUGE_LONG:
				methodName = "set";
				methodArgs = instrument.value != null
						? Arrays.asList(instrument.value)
						: Arrays.asList("0L");
			case GAUGE_DOUBLE:
				methodName = "set";
				methodArgs = instrument.value != null
						? Arrays.asList(instrument.value)
						: Arrays.asList("0.0");
			default:
				break;
		}

		final StringBuilder instrumentBody = new StringBuilder();

		final StringBuilder beforeBody = new StringBuilder();
		final StringBuilder afterBody = new StringBuilder();
		final StringBuilder catchBody = new StringBuilder();

		if (instrument.type == InstrumentType.TIMER
				|| (instrument.condition != null && instrument.condition.contains("$time0"))
				|| methodArgs.stream().anyMatch(arg -> arg.contains("$time0"))) {
			method.addLocalVariable("$time0", CtClass.longType);
			beforeBody.append("$time0 = System.nanoTime();");
		}

		instrumentBody.append("try {\n");

		if (instrument.condition != null) {
			instrumentBody.append("if (" + instrument.condition + ") {");
		}
		if (cacheField != null) {
			instrumentBody.append(Java.IfStatement(Java.BinaryOperator(constructFieldAccess(cacheField), "==", Java.Null()),
					Java.Statement(Java.Assignment(constructFieldAccess(cacheField), constructGetMetricsExpression(instrument.jmx, metricsClass)))));
			instrumentBody.append(Java.Statement(Java.FunctionCall(constructFieldAccess(cacheField), methodName, methodArgs)));
		} else {
			instrumentBody.append(Java.Statement(Java.FunctionCall(constructGetMetricsExpression(instrument.jmx, metricsClass), methodName, methodArgs)));
		}
		if (instrument.condition != null) {
			instrumentBody.append("}");
		}

		instrumentBody.append("} catch (Throwable th) {\n");
		instrumentBody.append("    if (!$0." + errorField.getName() + ") {\n");
		instrumentBody.append("        th.printStackTrace();\n");
		instrumentBody.append("        $0." + errorField.getName() + " = true;\n");
		instrumentBody.append("    }\n");
		instrumentBody.append("}\n");

		switch (instrument.event) {
			case ON_ENTER:
				beforeBody.append(instrumentBody.toString());
				break;
			case ON_RETURN:
				afterBody.append(instrumentBody.toString());
				break;
			case ON_EXCEPTION:
				catchBody.append(instrumentBody.toString());
				catchBody.append("throw $th;");
				break;
			default:
				throw new RuntimeException("not implemented");
		}

		if (beforeBody.length() > 0) {
			System.err.println("BEFORE " + instrument.method.toString() + ": " + beforeBody.toString());
			method.insertBefore(beforeBody.toString());
		}
		if (afterBody.length() > 0) {
			System.err.println("AFTER " + instrument.method.toString() + ": " + afterBody.toString());
			method.insertAfter(afterBody.toString());
		}
		if (catchBody.length() > 0) {
			System.err.println("CATCH " + instrument.method.toString() + ": " + catchBody.toString());
			method.addCatch(catchBody.toString(), cp.get("java.lang.Throwable"), "$th");
		}
	}

	public static ClassPool createClassPool(final JvmAgentConfig config) {
		final ClassPool cp = new ClassPool(true);
		for (final String classpath : config.classpaths) {
			try {
				cp.appendClassPath(classpath);
			} catch (final NotFoundException e1) {
				e1.printStackTrace(); // FIXME
			}
		}
		return cp;
	}

}