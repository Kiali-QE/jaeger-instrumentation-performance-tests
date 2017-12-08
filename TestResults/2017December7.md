# Jaeger/OpenTracing Instrumentation Performance Test Run 7 December 2017

**Host** 
+ `Lenovo T540`
+ `Memory 16G`
+ `Intel Core i7-4900MQ CPU @ 2.80Ghz x 8`
+ `Disk 250gb SSD`
+ `Ubuntu 17.10`

**Minishift Version**  `v1.9.0+a511b25` 
Minishift run with `minishift start --cpus=6 --memory=12288 --disk-size=80GB`

**OpenShift Version:** `v3.6.0+c4dd4cf`

**Kubernetes Master:** `v1.6.1+5115d708d7`

**Number of test clients** `100`

**Iterations** `10000`

## Application Dependency Versions
### Wildfly Swarm
+ wildfly-swarm: 2017.11.0
+ opentracing-jaxrs2: 0.0.9

### Spring Boot
+ spring-boot-starter: 1.5.6.RELEASE
+ spring-cloud: Dalston.SR3
+ opentracing-spring-cloud-starter: 0.0.7

### Vert-x
+ vertx: 3.5.0
+ opentracing-vertx-web: 0.0.1

## Results

The results shown below are throughput reported by JMeter in operations per second (i.e. a GET on an endpoint = 1 operation.)  
For this test run there are 2 operations per iteration for a total of 2,000,000 opertions.  The `/singleSpan` endpoing 
creates one trace, while the `/spanWithChild` endpoint creates 2 traces, so each test run creates a total of 3,000,000 traces. 

| App/Tracer|NONE| NOOP| JAEGER Collector Sampling 1.0 |JAEGER Collector(3 Pods) Sampling 1.0 | JAEGER Collector(6 Pods) Sampling 1.0 |
| ------------- | -----:|-----:|-----:|-----:|-----:|
| Wildfly-swarm| 9588,2 | 9333,6 | 3427,0 | 3252,4 | 3024,4 | 
| Spring Boot| 6050,3 | 5674,9 | 2294,4 | 2223,6 | 2009,8 |
| Vert-X| 13409,3 | 13002,2 | 5426,5 | 5467,0 | 4589,2 | 

## Evaluating the results
It's a bit difficult to give a simple answer on the cost of instrumentation as there are many factors to consider.  At
first glance, if for example we use the wildfly-swarm application, the cost looks significant, as we go from 9588,2 
operations per second to 3427,0.  However it's important to keep in mind that we're using a very simplistic test application
that does very little work.  For a real application the difference would be less.

More importantly, in the wildfly-swarm case (assuming I'm doing the math correctly) instrumentation adds only 0,000187506 
seconds to the time required per operation.

## A note on consistency
These tests were run on a standalone machine running only minishift, one instance of a Chrome browser with only one
tab open, and one shell window.  (The laptop was connected to my home network).  To get an insight into getting consistent
results, I first ran the test five times using the wildfly-swarm example and 1 collector instance.  I got the following results for
throughput:

+ 3069,0
+ 3223,6
+ 3280,4
+ 3304,3
+ 3528,4

The average here is 3281.4, and the range went from -0.065% below to +0.075% above the average.  This is in line with what
another member of the JON QE team, Filip Brychta, told me he has seen in performance testing on OpenShift.









