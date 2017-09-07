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
                    currentBuild.displayName = env.TARGET_APP + " " + env.TRACER_TYPE + " " + env.JMETER_CLIENT_COUNT + " " + env.ITERATIONS + " " + env.JAEGER_SAMPLING_RATE
                    currentBuild.description = env.TARGET_APP + " " + env.TRACER_TYPE + " " + env.JMETER_CLIENT_COUNT + " clients " + env.ITERATIONS + " iterations " + env.JAEGER_SAMPLING_RATE + " sampling"
                }
            }
        }
        stage('Delete Jaeger') {
            steps {
                sh 'oc delete all,template,daemonset,configmap -l jaeger-infra'
            }
        }
        /*stage('Delete example app') {
            steps {
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    sh 'mvn -f ${TARGET_APP}/pom.xml -Popenshift fabric8:undeploy'
                }
            }
        }*/
        stage('Cleanup workspace') {
            steps {
                sh 'ls -alF'
            }
        }
        stage('deploy Jaeger Production Template') {
            steps {
                sh 'oc process -f https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/jaeger-production-template.yml | oc create -n jaeger-infra -f -'
            }
        }
        stage('verify Jaeger deployment'){
            steps{
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-query', verbose: 'false'
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-collector', verbose: 'false'
            }
        }
        stage('Deploy example application'){
            steps{
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    script {
                        if (env.TRACER_TYPE == 'JAEGER') {
                            git 'https://github.com/kevinearls/jaeger-performance-tests.git'
                        } else {
                            git branch: 'no-tracing', url: 'https://github.com/kevinearls/jaeger-performance-tests.git'
                        }
                    }
                    sh 'git status'
                    sh 'mvn --file ${TARGET_APP}/pom.xml --activate-profiles openshift fabric8:deploy -Djaeger.sampling.rate=${JAEGER_SAMPLING_RATE} -Djaeger.agent.host=${JAEGER_AGENT_HOST}'
                }
            }
        }
        stage('verify example deployment'){
            steps{
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: env.testTargetApp, verbose: 'false'
            }
        }
        stage ('scale example deployment') {
                    steps {
                        openshiftScale apiURL: '', authToken: '', depCfg: env.testTargetApp, namespace: '', replicaCount: env.EXAMPLE_PODS, verbose: 'true', verifyReplicaCount: 'true', waitTime: ''
                    }
                }
        stage('Run JMeter Test') {
            steps{
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    sh '''
                        rm -rf apache-jmeter*
                        curl  http://mirrors.standaloneinstaller.com/apache//jmeter/binaries/apache-jmeter-3.2.tgz --output apache-jmeter-3.2.tgz
                        gunzip apache-jmeter-3.2.tgz
                        tar -xf apache-jmeter-3.2.tar
                        rm -rf log.txt reports

                        export PORT=8080
                        ./apache-jmeter-3.2/bin/jmeter --nongui --testfile TestPlans/SimpleTracingTest.jmx -JTHREADCOUNT=${JMETER_CLIENT_COUNT} -JITERATIONS=${ITERATIONS} -JRAMPUP=${RAMPUP} -JURL=${JMETER_URL} -JPORT=${PORT} -JDELAY1=${DELAY1} -JDELAY2=${DELAY2} --logfile log.txt --reportatendofloadtests --reportoutputfolder reports

                    '''
                }
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'reports', reportFiles: 'index.html', reportName: 'Performance Report', reportTitles: ''])
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