package net.thisptr.java.method.stats.agent.cofig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JvmAgentConfig {
	@JsonProperty("instruments")
	public List<InstrumentConfig> instruments = new ArrayList<>();

	@JsonProperty("classpaths")
	public List<String> classpaths = new ArrayList<>();

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class InstrumentConfig {
		@JsonProperty("type")
		public InstrumentType type = InstrumentType.COUNTER;

		@JsonProperty("event")
		public InstrumentEvent event = InstrumentEvent.ON_ENTER;

		@JsonProperty("condition")
		public String condition = null;

		@JsonProperty("value")
		public String value = null; // null means a default value which varies depending on InstrumentType to use

		@JsonProperty("method")
		public MethodName method;

		@JsonProperty("jmx")
		public JmxObjectName jmx = new JmxObjectName();

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class JmxObjectName {
			@JsonProperty("domain")
			public String domain;

			@JsonProperty("keys")
			public Map<String, String> keys = new HashMap<>();
		}

		public enum InstrumentType {
			GAUGE_LONG, GAUGE_DOUBLE, COUNTER, METER, HISTOGRAM, TIMER, UNREGISTER
		}

		public enum InstrumentEvent {
			ON_ENTER, ON_RETURN, ON_EXCEPTION, ON_COMPLETABLE_FUTURE_COMPLETE, ON_COMPLETABLE_FUTURE_EXCEPTION;
		}

		@JsonProperty("cache_scope")
		public CacheScope cacheScope = CacheScope.INSTANCE;

		public enum CacheScope {
			CLASS, INSTANCE
		}
	}

	private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

	public static JvmAgentConfig fromString(final String args) throws JsonParseException, JsonMappingException, IOException {
		if (args == null || args.trim().isEmpty())
			return new JvmAgentConfig();
		if (args.startsWith("@")) {
			final JvmAgentConfig config = new JvmAgentConfig();
			for (final String path : args.substring(1).split(",")) {
				final JvmAgentConfig fragment = MAPPER.readValue(new File(path), JvmAgentConfig.class);
				config.classpaths.addAll(fragment.classpaths);
				config.instruments.addAll(fragment.instruments);
			}
			return config;
		} else {
			return MAPPER.readValue(args, JvmAgentConfig.class);
		}
	}
}