java-method-stats-agent
=======================

Java agent for collecting and exposing method statistics via JMX.

**WARNING** (2017-07-01) I just started this project and should not be considered production-ready yet.

Usage
-----

```sh
java -javaagent:/path/to/java-method-stats-agent-$VERSION.jar=@/path/to/config.yaml ...
```

### Examples

#### Apache Flume

This instruments `append` and `appendBatch` RPC handler on Avro Source and collect their response time statistics which are not provided by Apache Flume out-of-the-box.

```yaml
instruments:
  - method: org.apache.flume.source.AvroSource.append(org.apache.flume.source.avro.AvroFlumeEvent)
    jmx:
      domain: org.apache.flume.source
      keys:
        type: ${$jmx.sanitize($0.getName())}
        method: append
    event: ON_RETURN
    type: TIMER
  - method: org.apache.flume.source.AvroSource.appendBatch(java.util.List)
    jmx:
      domain: org.apache.flume.source
      keys:
        type: ${$jmx.sanitize($0.getName())}
        method: appendBatch
    event: ON_RETURN
    type: TIMER
```

See [/examples](/examples) for more configuration examples.

License
-------

[The Apache License, Version 2.0](LICENSE)
