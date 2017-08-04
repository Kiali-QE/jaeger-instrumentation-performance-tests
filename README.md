# Wildfly Swarm with OpenTracing demo

This is a simple example based on http://www.hawkular.org/blog/2017/07/opentracing-jaxrs.html

Use `mvn wildfly-swarm:run` to run this, or `mvn package`, and then `java -jar target/demo-swarm.jar`

By default this will run on port 8080 and has 2 endpoints, *singleSpan* and *spanWithChild*

The following environment variables can be used to alter the default behavior:

+ TRACER_TYPE The NoopTracer will be used if this is set to any value other than "jaeger"
+ SLEEP_INTERVAL The number of milliseconds each action should sleep.  This defaults to 10.
+ TEST_SERVICE_NAME Service name to use when reporting spans to jaeger.  Defaults to "wildfly-swarm-opentracing-demo"

## Docker

To create a docker image `mvn clean install -Pdocker`

To run it `docker run -p 8080:8080 wildfly-swarm-opentracing`

## Openshift

To deploy to Openshift

+ Use the `oc` command to login to openshift 
+ `mvn `fabric8:deploy -Popenshift`

## JMeter

To run the performance test, use the following command: 
    `jmeter --nongui --testfile TestPlans/SimpleTracingTest.jmx\
        -JTHREADCOUNT=100 -JITERATIONS=1000 -JRAMPUP=0 \
        -JURL=localhost -JPORT=8080 --logfile log.txt \ 
        --reportatendofloadtests --reportoutputfolder reports`
        
+ *THREADCOUNT* is the number of client threads to run
+ *ITERATIONS* is the number of iterations each client will make
+ *RAMPUP* is the number of seconds taken to start all clients
+ *URL* is the server URL, defaults to *localhost*
+ *PORT* is the port, defaults to *8080*

Each iteration of tthe testing defined in `TestPlan/SimpleTracingTest.jmx` will hit 2 urls, `singleSpan`
and `/spanWithChild`.  In addition to the variables above, there are two others that can be set to create a delay after each endpoing
+ *DELAY1* sets a delay after hitting `/singleSpan`, default is *5* milliseconds
+ *DELAY2* sets a delay after hitting `/spanWithChild`, default is *5* milliseconds


 