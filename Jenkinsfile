pipeline {
    agent any

    parameters {
        choice(choices: 'JAEGER\nNOOP\nNONE', description: 'Which tracer to use', name: 'TRACER_TYPE')
        choice(choices: 'wildfly-swarm\nspring-boot\nvertx', description: 'Which target application to run against', name: 'TARGET_APP')
        string(name: 'JAEGER_AGENT_HOST', defaultValue: 'localhost', description: 'Host where the agent is running')
        string(name: 'JAEGER_SAMPLING_RATE', defaultValue: '1.0', description: '0.0 to 1.0 percent of spans to record')
        string(name: 'JAEGER_MAX_QUEUE_SIZE', defaultValue: '100', description: 'Tracer queue size')
        string(name: 'JMETER_CLIENT_COUNT', defaultValue: '100', description: 'The number of client threads JMeter should create')
        string(name: 'ITERATIONS', defaultValue: '1000', description: 'The number of iterations each client should execute')
        string(name: 'EXAMPLE_PODS', defaultValue: '1', description: 'The number of pods to deploy for the example application')
        string(name: 'RAMPUP', defaultValue: '0', description: 'The number of seconds to take to start all jmeter clients')
        string(name: 'DELAY1', defaultValue: '1', description: 'delay after hitting /singleSpan')
        string(name: 'DELAY2', defaultValue: '1', description: 'delay after hitting /spanWithChild')
        booleanParam(name: 'DELETE_JAEGER_AT_END', defaultValue: true, description: 'Delete Jaeger instance at end of the test')
        booleanParam(name: 'DELETE_EXAMPLE_AT_END', defaultValue: true, description: 'Delete the target application at end of the test')
    }
    environment {
        testTargetApp = 'jaeger-performance-' + "${TARGET_APP}" + '-app'
        JMETER_URL = "${testTargetApp}" + ".jaeger-infra.svc"
    }
    stages {
        stage('Set name and description') {
            steps {
                script {
                    currentBuild.displayName = params.TARGET_APP + " " + params.TRACER_TYPE + " " + params.JMETER_CLIENT_COUNT + " " + params.ITERATIONS + " " + params.JAEGER_SAMPLING_RATE + " QS: " + params.JAEGER_MAX_QUEUE_SIZE + " D1: " + params.DELAY1  + " D2: " + params.DELAY2
                    currentBuild.description = params.TARGET_APP + " " + params.TRACER_TYPE + " " + params.JMETER_CLIENT_COUNT + " clients " + params.ITERATIONS + " iterations " + params.JAEGER_SAMPLING_RATE + " sampling"
                }
            }
        }
        stage('Delete Jaeger') {
            steps {
                sh 'oc delete all,template,daemonset,configmap -l jaeger-infra'
            }
        }
        stage('Cleanup and checkout') {
            steps {
                deleteDir()
                script {
                    if (params.TRACER_TYPE != 'NONE') {
                        git 'https://github.com/Hawkular-QE/jaeger-instrumentation-performance-tests.git'
                    } else {
                        git branch: 'no-tracing', url: 'https://github.com/Hawkular-QE/jaeger-instrumentation-performance-tests.git'
                    }
                }
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    sh 'mvn -f ${TARGET_APP}/pom.xml -Popenshift fabric8:undeploy'
                }
            }
        }
        stage('Delete example app') {
            steps {
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    sh 'mvn -f ${TARGET_APP}/pom.xml -Popenshift fabric8:undeploy'
                }
            }
        }
        stage('deploy Jaeger') {
            when {
                expression { params.TRACER_TYPE == 'JAEGER'}
            }
            steps {
                sh 'oc process -f https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/jaeger-production-template.yml | oc create -n jaeger-infra -f -'
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-query', verbose: 'false'
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-collector', verbose: 'false'
            }
        }
        stage('Deploy example application'){
            steps{
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    sh 'git status'
                    sh 'mvn --file ${TARGET_APP}/pom.xml --activate-profiles openshift clean install fabric8:deploy -Djaeger.sampling.rate=${JAEGER_SAMPLING_RATE} -Djaeger.agent.host=${JAEGER_AGENT_HOST} -Djaeger.max.queue.size=${JAEGER_MAX_QUEUE_SIZE}'
                }
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: env.testTargetApp, verbose: 'false', retryCount:'200'
            }
        }
        stage('Run JMeter Test') {
            steps{
                sh '''

                    rm -rf log.txt reports
                    export PORT=8080
                    ~/tools/apache-jmeter-3.3/bin/jmeter --nongui --testfile TestPlans/SimpleTracingTest.jmx -JTHREADCOUNT=${JMETER_CLIENT_COUNT} -JITERATIONS=${ITERATIONS} -JRAMPUP=${RAMPUP} -JURL=${JMETER_URL} -JPORT=${PORT} -JDELAY1=${DELAY1} -JDELAY2=${DELAY2} --logfile log.txt --reportatendofloadtests --reportoutputfolder reports
                    '''
                script {
                    env.THROUGHPUT = sh (returnStdout: true, script: 'grep "summary =" jmeter.log | tail -1 | sed "s/^.*summary = //g" | sed "s/^.*= //g" | sed "s/\\/s.*//g"')
                    env.ERRORS = sh(returnStdout: true, script: 'grep "summary =" jmeter.log | tail -1 | sed "s/^.*Err:/Errors:/g"')

                    currentBuild.description = "Throughput: " + env.THROUGHPUT + " " + env.ERRORS
                 }
                 publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'reports', reportFiles: 'index.html', reportName: 'Performance Report', reportTitles: ''])
            }
        }
        stage('Validate Traces') {
            when {
                expression { params.TRACER_TYPE == 'JAEGER'}
            }
            steps{
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    sh 'mvn --file common/pom.xml -Dcluster.ip=cassandra -Dkeyspace.name=jaeger_v1_dc1 -Dquery="SELECT COUNT(*) FROM traces" clean test'
                }
                script {
                    env.TRACE_COUNT=readFile 'common/traceCount.txt'
                    currentBuild.description = currentBuild.description + " Trace count " + env.TRACE_COUNT
                }
            }
        }
        stage('Delete Jaeger at end') {
            when {
                expression { params.TRACER_TYPE == 'JAEGER' && params.DELETE_JAEGER_AT_END  }
            }
            steps {
                script {
                    sh 'oc delete all,template,daemonset,configmap -l jaeger-infra'
                }
            }
        }
        stage('Delete example app at end') {
            when {
                expression { params.DELETE_EXAMPLE_AT_END }
            }
            steps {
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    script {
                        sh 'mvn -f ${TARGET_APP}/pom.xml -Popenshift fabric8:undeploy'
                    }
                }
            }
        }
    }
}