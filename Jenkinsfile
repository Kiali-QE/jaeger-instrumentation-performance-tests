pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }
    tools {
        maven 'maven-3.5.3'
        jdk 'jdk8'
    }
    parameters {
        choice(choices: 'JAEGER\nNOOP\nNONE', description: 'Which tracer to use', name: 'TRACER_TYPE')
        choice(choices: 'wildfly-swarm\nspring-boot\nvertx', description: 'Which target application to run against', name: 'TARGET_APP')
        choice(choices: 'COLLECTOR\nAGENT', description: 'Write spans to the agent or the collector', name: 'USE_AGENT_OR_COLLECTOR')
        choice(choices: 'cassandra\nelasticsearch', description: 'Span Storage', name: 'SPAN_STORAGE_TYPE')
        string(name: 'JMETER_CLIENT_COUNT', defaultValue: '100', description: 'The number of client threads JMeter should create')
        string(name: 'ITERATIONS', defaultValue: '1000', description: 'The number of iterations each client should execute')
        string(name: 'JAEGER_AGENT_HOST', defaultValue: 'localhost', description: 'Host where the agent is running')
        string(name: 'JAEGER_COLLECTOR_HOST', defaultValue: 'jaeger-collector', description: 'Host where the collector is running')   // FIXME
        string(name: 'JAEGER_COLLECTOR_PORT', defaultValue: '14268', description: 'Collector port')
        string(name: 'JAEGER_SAMPLING_RATE', defaultValue: '1.0', description: '0.0 to 1.0 percent of spans to record')
        string(name: 'JAEGER_MAX_QUEUE_SIZE', defaultValue: '300000', description: 'Tracer queue size')
        string(name: 'KEYSPACE_NAME', defaultValue: 'jaeger_v1_dc1', description: 'Name of the Jaeger keyspace in Cassandra')
        string(name: 'ELASTICSEARCH_HOST', defaultValue: 'elasticsearch', description: 'ElasticShift host')
        string(name: 'ELASTICSEARCH_PORT', defaultValue: '9200', description: 'ElasticShift port')
        string(name: 'EXAMPLE_PODS', defaultValue: '1', description: 'The number of pods to deploy for the example application')
        string(name: 'COLLECTOR_PODS', defaultValue: '1', description: 'The number of pods to deploy for the Jaeger Collector')
        string(name: 'RAMPUP', defaultValue: '0', description: 'The number of seconds to take to start all jmeter clients')
        string(name: 'DELAY1', defaultValue: '1', description: 'delay after hitting /singleSpan')
        string(name: 'DELAY2', defaultValue: '1', description: 'delay after hitting /spanWithChild')
        booleanParam(name: 'DELETE_JAEGER_AT_END', defaultValue: true, description: 'Delete Jaeger instance at end of the test')
        booleanParam(name: 'DELETE_EXAMPLE_AT_END', defaultValue: true, description: 'Delete the target application at end of the test')
    }
    environment {
        testTargetApp = 'jaeger-performance-' + "${TARGET_APP}" + '-app'
    }
    stages {
        stage('Set name and description') {
            steps {
                script {
                    currentBuild.displayName = params.TARGET_APP + " " + params.TRACER_TYPE + " " + params.USE_AGENT_OR_COLLECTOR +
                         " " + params.SPAN_STORAGE_TYPE + " " + params.JMETER_CLIENT_COUNT + " " + params.ITERATIONS + " " + params.JAEGER_SAMPLING_RATE + " QS: "  +
                        params.JAEGER_MAX_QUEUE_SIZE + " D1: " + params.DELAY1  + " D2: " + params.DELAY2
                    currentBuild.description = currentBuild.displayName
                }
            }
        }
        stage('Delete Jaeger') {
            steps {
                sh 'oc delete all,template,daemonset,configmap -l jaeger-infra'
                sh 'env | sort'
            }
        }
        stage('Cleanup, checkout, build') /* Change to checkout scm */ {
            steps {
                deleteDir()
                script {
                    if (params.TRACER_TYPE != 'NONE') {
                        git 'https://github.com/Hawkular-QE/jaeger-instrumentation-performance-tests.git'
                    } else {
                        git branch: 'no-tracing', url: 'https://github.com/Hawkular-QE/jaeger-instrumentation-performance-tests.git'
                    }
                }
                /* We need to build here so stuff in common wil be available */
                sh 'mvn -DskipITs clean install'
            }
        }
        stage('Delete example app') {
            steps {
                sh 'mvn -f ${TARGET_APP}/pom.xml -Popenshift fabric8:undeploy'
            }
        }
        stage('deploy Cassandra') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'cassandra'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/cassandra.yml --output cassandra.yml
                    oc create --filename cassandra.yml
                '''
            }
        }
        stage('deploy ElasticSearch') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'elasticsearch'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/elasticsearch.yml --output elasticsearch.yml
                    oc create --filename elasticsearch.yml
                '''
            }
        }
        stage('deploy Jaeger with Cassandra') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'cassandra' && params.TRACER_TYPE == 'JAEGER' }
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/configmap-cassandra.yml --output configmap-cassandra.yml
                    oc create -f configmap-cassandra.yml
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/jaeger-production-template.yml --output jaeger-production-template.yml
                    ./updateTemplateForCassandra.sh
                    oc process  ${DEPLOYMENT_PARAMETERS} -f ./jaeger-production-template.yml  | oc create -n ${PROJECT_NAME} -f -
                '''
            }
        }
        stage('deploy Jaeger with ElasticSearch') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'elasticsearch'  && params.TRACER_TYPE == 'JAEGER'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/configmap-elasticsearch.yml --output configmap-elasticsearch.yml
                    oc create -f configmap-elasticsearch.yml
                    curl https://raw.githubusercontent.com/kevinearls/jaeger-openshift/working/production/jaeger-production-template.yml --output jaeger-production-template.yml
                    ./updateTemplateForElasticSearch.sh
                    oc process ${DEPLOYMENT_PARAMETERS} -pES_BULK_SIZE=${ES_BULK_SIZE} -pES_BULK_WORKERS=${ES_BULK_WORKERS} -pES_BULK_FLUSH_INTERVAL=${ES_BULK_FLUSH_INTERVAL} -f jaeger-production-template.yml  | oc create -n ${PROJECT_NAME} -f -
                '''
            }
        }

        stage('Deploy example application'){
            steps{
                sh 'git status'
                sh 'mvn --file ${TARGET_APP}/pom.xml --activate-profiles openshift clean install fabric8:deploy -Djaeger.sampling.rate=${JAEGER_SAMPLING_RATE} -Djaeger.agent.host=${JAEGER_AGENT_HOST} -Djaeger.max.queue.size=${JAEGER_MAX_QUEUE_SIZE} -Duser.agent.or.collector=${USE_AGENT_OR_COLLECTOR} -Djaeger.collector.port=${JAEGER_COLLECTOR_PORT} -Djaeger.collector.host=${JAEGER_COLLECTOR_HOST}'
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: env.testTargetApp, verbose: 'false', retryCount:'200'
                /* Hack to make sure app is started before starting JMeter */
                sleep 90
                sh 'curl ${testTargetApp}"."${PROJECT_NAME}".svc:8080"'
            }
        }
        stage('Run JMeter Test') {
            steps{
                sh '''
                    if [ ! -e /var/lib/jenkins/tools/apache-jmeter-4.0/bin/jmeter ]; then
                        cd ~/tools
                        curl http://apache.mediamirrors.org//jmeter/binaries/apache-jmeter-4.0.tgz --output apache-jmeter-4.0.tgz
                        gunzip apache-jmeter-4.0.tgz
                        tar -xvf apache-jmeter-4.0.tar
                        rm apache-jmeter-4.0.tar
                        ls -alF
                    fi

                    rm -rf log.txt reports
                    export PORT=8080
                    export JMETER_URL=${testTargetApp}"."${PROJECT_NAME}".svc"
                    ~/tools/apache-jmeter-4.0/bin/jmeter --nongui --testfile TestPlans/SimpleTracingTest.jmx -JTHREADCOUNT=${JMETER_CLIENT_COUNT} -JITERATIONS=${ITERATIONS} -JRAMPUP=${RAMPUP} -JURL=${JMETER_URL} -JPORT=${PORT} -JDELAY1=${DELAY1} -JDELAY2=${DELAY2} --logfile log.txt --reportatendofloadtests --reportoutputfolder reports
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
                sh 'rm -f common/traceCount.txt'
                sh 'mvn --file common/pom.xml -Dcluster.ip=cassandra -Dkeyspace.name=jaeger_v1_dc1 clean integration-test'
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
                script {
                    sh 'mvn -f ${TARGET_APP}/pom.xml -Popenshift fabric8:undeploy'
                }
            }
        }
        stage('Cleanup pods') {
            steps {
                script {
                    sh 'oc get pods | grep Completed | awk {"print \\$1"} | xargs oc delete pod || true'
                }
            }
        }
    }
}