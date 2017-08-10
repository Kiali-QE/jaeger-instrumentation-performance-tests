pipeline {
    agent any

    stages {
        stage('Set name and desription') {
            steps {
                script {
                    currentBuild.displayName = env.TRACER_TYPE + " " + env.JMETER_CLIENT_COUNT + " " + env.ITERATIONS
                    currentBuild.description = env.TRACER_TYPE + " " + env.JMETER_CLIENT_COUNT + " clients " + env.ITERATIONS + " iterations"
                }
            }
        }
        stage('Delete Jaeger') {
            steps {
                sh 'oc delete all,template,daemonset,configmap -l jaeger-infra'
            }
        }
        stage('Delete wildfly-swarm example app') {
            steps {
                sh 'oc delete all,template,daemonset,configmap -l project=wildfly-swarm-opentracing'
            }
        }
        stage('deploy Jaeger') {
            steps {
                sh 'oc process -f https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/all-in-one/jaeger-all-in-one-template.yml | oc create -f -'
            }
        }
        stage('verify Jaeger deployment'){
            steps{
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-query', verbose: 'false'
            }
        }
        stage('Deploy wildfly-swarm example'){
            steps{
                withEnv(["JAVA_HOME=${ tool 'jdk8' }", "PATH+MAVEN=${tool 'maven-3.5.0'}/bin:${env.JAVA_HOME}/bin"]) {
                    script {
                        if (env.TRACER_TYPE == 'JAEGER') {
                            git 'https://github.com/kevinearls/wildfly-swarm-opentracing-demo.git'
                        } else {
                            git branch: 'no-tracing', url: 'https://github.com/kevinearls/wildfly-swarm-opentracing-demo.git'
                        }
                    }
                    sh 'git status'
                    sh 'mvn fabric8:deploy -DJAEGER_SAMPLING_RATE=0.25 -Popenshift'
                }
            }
        }
        stage('verify wildfly swarm example deployment'){
            steps{
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'wildfly-swarm-opentracing', verbose: 'false'
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

                        export URL=wildfly-swarm-opentracing.jaeger-performance.svc
                        export PORT=8080
                        ./apache-jmeter-3.2/bin/jmeter --nongui --testfile TestPlans/SimpleTracingTest.jmx -JTHREADCOUNT=${JMETER_CLIENT_COUNT} -JITERATIONS=${ITERATIONS} -JRAMPUP=${RAMPUP} -JURL=${URL} -JPORT=${PORT} -JDELAY1=${DELAY1} -JDELAY2=${DELAY2} --logfile log.txt --reportatendofloadtests --reportoutputfolder reports

                    '''
                }
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'reports', reportFiles: 'index.html', reportName: 'Performance Report', reportTitles: ''])
            }
        }
    }
}