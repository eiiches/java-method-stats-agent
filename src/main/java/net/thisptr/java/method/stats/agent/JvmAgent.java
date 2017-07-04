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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import net.thisptr.java.method.stats.agent.config.JvmAgentConfig;
import net.thisptr.java.method.stats.agent.config.JvmAgentConfig.InstrumentConfig;
import net.thisptr.java.method.stats.agent.config.JvmAgentConfig.InstrumentConfig.CacheScope;
import net.thisptr.java.method.stats.agent.config.JvmAgentConfig.InstrumentConfig.JmxObjectName;
import net.thisptr.java.method.stats.agent.config.MethodName;
import net.thisptr.java.method.stats.agent.misc.Java;
import net.thisptr.java.method.stats.agent.misc.JavassistUtils;
import net.thisptr.java.method.stats.agent.misc.ObjectNameUtils;
import net.thisptr.java.method.stats.agent.registry.MetricRegistry;
import net.thisptr.java.method.stats.agent.templates.CodeTemplate;
import net.thisptr.java.method.stats.agent.templates.CounterCodeTemplate;
import net.thisptr.java.method.stats.agent.templates.DoubleGaugeCodeTemplate;
import net.thisptr.java.method.stats.agent.templates.HistogramCodeTemplate;
import net.thisptr.java.method.stats.agent.templates.LongGaugeCodeTemplate;
import net.thisptr.java.method.stats.agent.templates.MeterCodeTemplate;
import net.thisptr.java.method.stats.agent.templates.TimerCodeTemplate;

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

							final CodeTemplate code;
							switch (instrument.type) {
								case COUNTER:
									code = new CounterCodeTemplate(instrument.value);
									break;
								case GAUGE_DOUBLE:
									code = new DoubleGaugeCodeTemplate(instrument.value);
									break;
								case GAUGE_LONG:
									code = new LongGaugeCodeTemplate(instrument.value);
									break;
								case HISTOGRAM:
									code = new HistogramCodeTemplate(instrument.value);
									break;
								case METER:
									code = new MeterCodeTemplate(instrument.value);
									break;
								case TIMER:
									code = new TimerCodeTemplate();
									break;
								default:
									throw new RuntimeException("not implemented");
							}

							new DefaultMethodInstrumentation(instrument, code).instrument(cp, clazz, instrument.method);

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
			MetricRegistry.get(metricsClass, name.domain, keyvalues.toArray(new String[keyvalues.size()]));
		}
		return Java.Cast(metricsClass.getName(), Java.FunctionCall(MetricRegistry.class.getName(), "get", Arrays.asList(new String[] {
				metricsClass.getName() + ".class",
				Java.StringLiteral(name.domain),
				Java.NewArrayWithInitializer("String", keyvalueExprs),
		})));
	}

	public static interface MethodInstrument {
		void instrument(ClassPool cp, CtClass clazz, MethodName method) throws Exception;
	}

	public static class DefaultMethodInstrumentation implements MethodInstrument {
		private CodeTemplate code;
		private InstrumentConfig config;

		public DefaultMethodInstrumentation(final InstrumentConfig config, final CodeTemplate code) {
			this.code = code;
			this.config = config;
		}

		public static class Snippet {
			public static class LocalVariable {
				public final String name;
				public final String type;

				public LocalVariable(final String type, final String name) {
					this.type = type;
					this.name = name;
				}
			}

			public static class ClassField {
				public final String type;
				public final String name;
				public final int modifiers;

				public ClassField(final String type, final String name, final int modifiers) {
					this.type = type;
					this.name = name;
					this.modifiers = modifiers;
				}
			}

			public final List<ClassField> fields;
			public final List<LocalVariable> locals;
			public final String source;

			public Snippet(final String source, final List<ClassField> fields, final List<LocalVariable> locals) {
				this.source = source;
				this.fields = fields;
				this.locals = locals;
			}
		}

		public Snippet render() {
			final List<Snippet.ClassField> fields = new ArrayList<>();
			final List<Snippet.LocalVariable> locals = new ArrayList<>();

			final String uuid = UUID.randomUUID().toString().replaceAll("-", "");

			// FIXME: avoid adding static fields on user classes
			fields.add(new Snippet.ClassField("boolean", "$$e_" + uuid, Modifier.STATIC));
			final String suppressErrorRef = "$0.$$e_" + uuid;

			final StringBuilder source = new StringBuilder();
			source.append("try {\n");
			if (config.condition != null) {
				source.append("    if (" + config.condition + ") {\n");
			}
			if (config.cacheScope != null) {
				final String instanceRef = "$0.$$m_" + uuid;
				fields.add(new Snippet.ClassField(code.clazz().getName(), "$$m_" + uuid, config.cacheScope == CacheScope.CLASS ? Modifier.STATIC : 0));
				source.append("        if (" + instanceRef + " == null)\n");
				source.append("            " + instanceRef + " = " + constructGetMetricsExpression(config.jmx, code.clazz()) + ";\n");
				source.append("        " + code.create(instanceRef) + "\n");
			} else {
				final String instanceRef = "$$m_" + uuid;
				locals.add(new Snippet.LocalVariable(code.clazz().getName(), instanceRef));
				source.append("        " + instanceRef + " = " + constructGetMetricsExpression(config.jmx, code.clazz()) + ";");
				source.append("        " + code.create(instanceRef) + "\n");
			}
			if (config.condition != null) {
				source.append("    }\n");
			}
			source.append("} catch (Throwable th) {\n");
			source.append("    if (!" + suppressErrorRef + ") {\n");
			source.append("        th.printStackTrace();\n");
			source.append("        " + suppressErrorRef + " = true;\n");
			source.append("    }\n");
			source.append("}\n");

			return new Snippet(source.toString(), fields, locals);
		}

		@Override
		public void instrument(final ClassPool cp, final CtClass clazz, final MethodName methodName) throws CannotCompileException, NotFoundException {

			final CtMethod method = JavassistUtils.findMethod(clazz, methodName);
			if (method == null) {
				System.err.println("Cannot find method: " + methodName);
				return;
			}
			System.err.println(methodName);

			final StringBuilder beforeBody = new StringBuilder();
			final StringBuilder afterBody = new StringBuilder();
			final StringBuilder catchBody = new StringBuilder();

			final Snippet snippet = render();

			// create necessary fields
			for (final Snippet.ClassField field : snippet.fields) {
				final CtField f = new CtField(cp.get(field.type), field.name, clazz);
				f.setModifiers(field.modifiers | Modifier.TRANSIENT);
				clazz.addField(f);
			}
			// create necessary local variables
			for (final Snippet.LocalVariable local : snippet.locals) {
				method.addLocalVariable(local.name, cp.get(local.type));
			}

			if (snippet.source.contains("$time0")) {
				method.addLocalVariable("$time0", CtClass.longType);
				beforeBody.append("$time0 = System.nanoTime();");
			}

			switch (config.event) {
				case ON_ENTER:
					beforeBody.append(snippet.source);
					break;
				case ON_RETURN:
					afterBody.append(snippet.source);
					break;
				case ON_EXCEPTION:
					catchBody.append(snippet.source);
					catchBody.append("throw $th;");
					break;
				default:
					throw new RuntimeException("not implemented");
			}

			if (beforeBody.length() > 0) {
				System.err.println("BEFORE " + methodName + ": \n" + beforeBody.toString());
				method.insertBefore(beforeBody.toString());
			}
			if (afterBody.length() > 0) {
				System.err.println("AFTER " + methodName + ": \n" + afterBody.toString());
				method.insertAfter(afterBody.toString());
			}
			if (catchBody.length() > 0) {
				System.err.println("CATCH " + methodName + ": \n" + catchBody.toString());
				method.addCatch(catchBody.toString(), cp.get("java.lang.Throwable"), "$th");
			}
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