# Wildfly Swarm with OpenTracing demo

This is a simple example based on http://www.hawkular.org/blog/2017/07/opentracing-jaxrs.html

Use `mvn wildfly-swarm:run` to run this, or `mvn package`, and then `java -jar target/demo-swarm.jar`

By default this will run on port 8080 and has 2 endpoints, *singleSpan* and *spanWithChild*

The following environment variables can be used to alter the default behavior:

+ TRACER_TYPE The NoopTracer will be used if this is set to any value other than "jaeger"
+ SLEEP_INTERVAL The number of milliseconds each action should sleep.  This defaults to 10.
+ TEST_SERVICE_NAME Service name to use when reporting spans to jaeger.  Defaults to "wildfly-swarm-opentracing-demo"

