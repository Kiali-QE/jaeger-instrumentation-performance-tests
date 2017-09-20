# Jaeger/OpenTracing Instrumentation Performance Test Run 18 September 2017

**Host** `QE Openshift at b20.jonqe.lab.eng.bos.redhat.com:8443`
**Number of test clients** `100`
**Iterations** `10000`

The results shown below are throughput in operations per seconds (i.e. a GET on and endpoint.)  For this test run there are 2 operations per iteration * 100 clients * 10000 iterations for a total of 2,000,000. 

| App/Tracer|NONE| NOOP/0.05| NOOP/0.1 | NOOP/0.2 | NOOP/1.0 |
| ------------- | -----:|-----:|-----:|-----:|-----:|
| Wildfly-swarm|18169,60|18743,80|19272,30|19733,40|18823,40|
| Spring Boot|15983,20|15692,60|16070,60|16206,70|16117,70| 
| Vert-X|17425,90|18155,10|18193,10|18029,60|17822,00|

| App/Tracer|JAEGER/0.05|JAEGER/0.1|JAEGER/0.2|JAEGER/1.0|
| ------------- | -----:|-----:|-----:|-----:|
| Wildfly-swarm|11746,30|12422,30|12249,90|12441,90|  
| Spring Boot|9248,70|8593,10|8662,70|9026,60| 
| Vert-X|12806,80|12440,10|11999,60|11389,70|


