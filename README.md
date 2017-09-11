# Jaeger/OpenTracing Instrumentation Performance Test

The purpose of this project is to provide tests and test applications which can be used to determine the performance 
costs of instrumenting a Wildfly Swarm, Spring Boot, or Vert-X application using OpenTracing/Jaeger

The project includes simple example for each of these frameworks which is based on 
http://www.hawkular.org/blog/2017/07/opentracing-jaxrs.html and can be used to do 
basic performance testing.  Tests can be run on a desktop, using Docker, or on OpenShift.  Running in any of 
these environments requires the following steps:

+ Start a Jaeger instance
+ Build and run the desired application
+ Run the JMeter test found in TestPlans/SimpleTracingTest

The test applications are very simple and each has the same 2 endpoints, *singleSpan* and *spanWithChild*  The following 
environment variables can be used to alter the default behavior:
                                             
+ **TRACER_TYPE** The NoopTracer will be used if this is set to any value other than "jaeger"
+ **TEST_SERVICE_NAME** Service name to use when reporting spans to jaeger.  Defaults to "framework-name-opentracing-demo", e.g. "vertx-opentracing-demo"
+ **JAEGER_SAMPLING_RATE** Set between 0.0 and 1.0 to set the sampling rate
+ **JAEGER_AGENT_HOST** Host the jaeger agent is running on, defaults to _localhost_

# Install JMeter
Apache JMeter is used for testing.  Download the latest instance from http://jmeter.apache.org/download_jmeter.cgi and add it to your PATH

# Running the tests standalone

+ Start Jaeger.  The simplest way to do this is to run the Jaeger all-in-one Docker image
   + `docker run -d -p5775:5775/udp -p6831:6831/udp -p6832:6832/udp -p5778:5778 -p16686:16686 -p14268:14268 jaegertracing/all-in-one:latest`
+ Build the example using `mvn -f <framework-dir>/pom.xml clean install`
+ Run the application using its maven plugin or fat jar:
    + `mvn -f wildfly-swarm/pom.xml wildfly-swarm:run` OR `java -jar wildfly-swarm/target/jaeger-performance-wildfly-swarm-app-swarm.jar `
    + `mvn -f spring-boot/pom.xml spring-boot:run` OR `java -jar spring-boot/target/jaeger-performance-spring-boot-app.jar`
    + `mvn -f vertx/pom.xml vertx:run` OR `java -jar vertx/target/jaeger-performance-vertx-app.jar `

+ Run the JMeter test  (Options are described below) 
    + `jmeter --nongui --testfile TestPlans/SimpleTracingTest.jmx -JTHREADCOUNT=100 -JITERATIONS=1000 -JRAMPUP=0 -JURL=localhost -JPORT=8080 --logfile log.txt --reportatendofloadtests --reportoutputfolder reports`
+ Open reports/index.html in a browser to view the results             

## Running the test application in Docker

If you'd prefer, you can run any of the example applications in Docker.  To do so replace the build
and run steps from the previous section with these steps

+ Create a docker image: `mvn -f <framework>/pom.xml clean install -Pdocker`
+ Run the application: `docker run -p 8080:8080 -eJAEGER_AGENT_HOST=${jaeger-host-ip} jaeger-performance-<framework>-app`

Note that `jaeger-host-ip` should be the real ip of the machine where you're running Jaeger, not `localhost`

## Running on OpenShift

This section describes how to set up a Jenkins job and run the tests on an OpenShift instance.  At the
current time, there are hard-coded dependencies on the project name, so the first thing you'll need to
do is create a project named `jaeger-infra`

+ `oc new-project jaeger-infra`

### Jenkins Creation

Next create and configure a Jenkins instance on OpenShift.  Log onto OpenShift in a browser, 
select the `jaeger-infra` project, and do the following:

+ Click on `Add to Project`
+ Select 'Continuous Integration and Deployment'
+ Select `Jenkins Ephemeral` (Or `Jenkins-Persistent` if you have persistent volumes set up)
+ Change the `Memory Limit` to `1024Mi`
+ Click `Create`

It may take 5 minutes or so until you can login to Jenkins.

### Jenkins Configuration

Once you can log onto Jenkins, there are two further configuration steps

+ Click on `Manage Jenkins` and then `Global Tool Configuration`
    + Add the latest JDK 8 and name it `jdk8`
    + Add Maven 3.5.0 and name it `maven-3.5.0`
+ Go back to `Manage Jenkins` and then click on `Manage Plugins`
    + Go to the `Available` tab, enter `HTML` in the Filter box, select `HTML Publisher plugin` and 
    then click on `Install without restart`
    + Go to the `Updates` tab, select all items (sorry, the UI for this is terrible) then click on 
    `Download now and install after restart.`
    + Click on the `Restart Jenkins...` box when it appears.  
    
Wait 5 minutes or so for Jenkins to restart so you can reconnect

### Creating the Jenkins job

Log back into Jenkins, and select `New Item`

+ Enter whatever name you'd like, select `Pipeline Script`, and click `OK`
+ Select `This project is parameterized` and add the following parameters
    + *TRACER_TYPE* _Choice Parameter_ with values **JAEGER**, **NOOP**, or **NONE**
    + *TARGET_APP* _Choice Parameter_ with values **wildfly-swarm**, **spring-boot**, and **vertx**
    + *JAEGER_AGENT_HOST* _String_ default `localhost` description `Hostname for Jaeger Ageent`
    + *JAEGER_SAMPLING_RATE* _String_ default `1.0` description `0.0 to 1.0 percent of spans to record`
    + *JMETER_CLIENT_COUNT* _String_ default `50`  description `The number of client threads JMeter should create`
    + *ITERATIONS* _String_ default `1000` description `The number of iterations each client should execute`
    + *EXAMPLE_PODS* _String_ default `1` description 'The number of pods to deploy for the example application`
    + *RAMPUP* _String_ default `30` description 'The number of seconds to take to start all clients`
    + *DELAY1* _String_ default `5` description `delay after hitting /singleSpan`
    + *DELAY2* _String_ default `5` description `delay after hitting /spanWithChild`
    + *DELETE_JAEGER_AT_END* _Boolean_ default `true` description `Delete Jaeger instance at end of the test`
    + *DELETE_EXAMPLE_AT_END* _Boolean_ default `true` description `Delete the target application at end of the test`
+ Scroll down to the **Pipeline** section and select `Pipeline script from SCM`
+ Select `Git` as the SCM and enter `https://github.com/kevinearls/wildfly-swarm-opentracing-demo.git` as 
the repository URL

To run the test click the `Build with Parameters` link.  Change the default parameters to whatever you'd like.

### Viewing results

So far I have been unable to get the HTML Publisher Plugin to work correctly in Jenkins on OpenShift.  
In order to see the full results, after running a job on Jenkins, click on the 
`Performance Report` link on the left side of page, and then click on the `Zip` link on the top
right corner of that page.  Unzip the `Performance_Report.zip` file that is downloaded, and then
open `Performance_Report/index.html` in a browser to view the results.

## Details of the JMeter Test

To run the performance test, use the following command: 
    `jmeter --nongui --testfile TestPlans/SimpleTracingTest.jmx -JTHREADCOUNT=100 -JITERATIONS=1000 -JRAMPUP=0 -JURL=localhost -JPORT=8080 --logfile log.txt --reportatendofloadtests --reportoutputfolder reports`
        
+ *THREADCOUNT* is the number of client threads to run
+ *ITERATIONS* is the number of iterations each client will make
+ *RAMPUP* is the number of seconds taken to start all clients
+ *URL* is the host that the wildfly-swarm app is running on.  It defaults to *localhost*
+ *PORT* is the port, defaults to *8080*

Each iteration of the test defined in `TestPlan/SimpleTracingTest.jmx` will hit 2 urls, `singleSpan`
and `/spanWithChild`.  In addition to the variables above, there are two others that can be set to create a delay after each endpoing
+ *DELAY1* sets a delay after hitting `/singleSpan`, default is *5* milliseconds
+ *DELAY2* sets a delay after hitting `/spanWithChild`, default is *5* milliseconds




 