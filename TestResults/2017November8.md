# Jaeger/OpenTracing Instrumentation Performance Test Run 8 November 2017

**Host** `QE Openshift at b20.jonqe.lab.eng.bos.redhat.com`

**OpenShift Master:** `v3.6.173.0.37`

**Kubernetes Master:** `v1.6.1+5115d708d7`

**Number of test clients** `100`

**Iterations** `10000`

## Application Dependency Versions
### Wildfly Swarm
+ wildfly-swarm: 2017.10.1
+ opentracing-jaxrs2: 0.0.9

### Spring Boot
+ spring-boot-starter: 1.5.6.RELEASE
+ spring-cloud: Dalston.SR3
+ opentracing-spring-cloud-starter: 0.0.2  (See https://github.com/opentracing-contrib/java-spring-cloud/issues/74)

### Vert-x
+ vertx: 3.5.0
+ opentracing-vertx-web: 0.0.1

## Results

The results shown below are throughput in operations per seconds (i.e. a GET on and endpoint.)  For this test run there are 
2 operations per iteration which create 3 traces * 100 clients * 10000 iterations for a total of 3,000,000 traces. 

| App/Tracer|NONE| NOOP| JAEGER Collector 1.0 |JAEGER Collector(3 Pods) 1.0 | JAEGER Collector(6 Pods) 1.0 |
| ------------- | -----:|-----:|-----:|-----:|-----:|
| Wildfly-swarm|12976.1|11961.7| 7837.6 | 11313.5 | 0.0 | 
| Spring Boot|8921.0|8148.6| 6278.6 | 9733.3 |  0 |
| Vert-X|7742.6|7056.7| 7837.6 | 8860.1 | 0.0 | 


