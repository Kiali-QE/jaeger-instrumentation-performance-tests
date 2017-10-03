pipeline {
    agent any

    environment {
        testTargetApp = 'jaeger-performance-' + "${TARGET_APP}" + '-app'
        JMETER_URL = "${testTargetApp}" + ".jaeger-infra.svc"
    }
    stages {
        stage('Set name and description') {
            steps {
                script {
                    currentBuild.displayName = env.TARGET_APP + " " + env.TRACER_TYPE + " " + env.JMETER_CLIENT_COUNT + " " + env.ITERATIONS + " " + env.JAEGER_SAMPLING_RATE + " QS: " + env.JAEGER_MAX_QUEUE_SIZE + " D1: " + env.DELAY1  + " D2: " + env.DELAY2
                    currentBuild.description = env.TARGET_APP + " " + env.TRACER_TYPE + " " + env.JMETER_CLIENT_COUNT + " clients " + env.ITERATIONS + " iterations " + env.JAEGER_SAMPLING_RATE + " sampling"
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
                sh 'echo after delete'
                sh 'ls -alF'
                script {
                    if (env.TRACER_TYPE == 'JAEGER') {
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
            steps {
                script {
                    if (env.DELETE_JAEGER_AT_END == 'true') {
                        sh 'oc delete all,template,daemonset,configmap -l jaeger-infra'
                    }
                }
            }
        }
        stage('Delete example app at end') {
            steps {
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    script {
                        if (env.DELETE_EXAMPLE_AT_END == 'true') {
                            sh 'mvn -f ${TARGET_APP}/pom.xml -Popenshift fabric8:undeploy'
                        }
                    }
                }
            }
        }
    }
}