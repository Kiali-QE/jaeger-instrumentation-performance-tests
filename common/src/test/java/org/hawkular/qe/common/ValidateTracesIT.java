/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.qe.common;

import com.datastax.driver.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class ValidateTracesIT {
    private static Map<String, String> envs = System.getenv();

    private static final String ELASTICSEARCH_HOST = envs.getOrDefault("ELASTICSEARCH_HOST", "elasticsearch");
    private static final Integer ELASTICSEARCH_PORT = new Integer(envs.getOrDefault("ELASTICSEARCH_PORT", "9200"));
    private static final Integer ITERATIONS = new Integer(envs.getOrDefault("ITERATIONS", "100"));
    private static final String SPAN_STORAGE_TYPE = envs.getOrDefault("SPAN_STORAGE_TYPE", "cassandra");
    private static final Integer JMETER_CLIENT_COUNT = new Integer(envs.getOrDefault("JMETER_CLIENT_COUNT", "100"));

    private static int EXPECTED_TRACES = ITERATIONS * JMETER_CLIENT_COUNT * 3;

    private static String CLUSTER_IP;
    private static String KEYSPACE_NAME;
    private Cluster cluster;

    private static Logger logger = Logger.getLogger(ValidateTracesIT.class.getName());

    @BeforeClass
    public static void beforeClass() {
        if (SPAN_STORAGE_TYPE.equals("cassandra")) {
            CLUSTER_IP = System.getProperty("cluster.ip", "cassandra");
            KEYSPACE_NAME = System.getProperty("keyspace.name", "jaeger_v1_dc1");
            logger.info("Running with custer.ip [" + CLUSTER_IP + "] Keyspace [" + KEYSPACE_NAME + "]");
        }
    }

    @Before
    public void setup() {
        if (SPAN_STORAGE_TYPE.equals("cassandra")) {
            Cluster.Builder builder = Cluster.builder();
            builder.addContactPoint(CLUSTER_IP);
            cluster = builder.build();
        }
    }

    @After
    public void tearDown() {
        if (SPAN_STORAGE_TYPE.equals("cassandra")) {
            cluster.close();
        }
    }

    /**
     * Try to confirm that the expected number of traces was created.  At this time we need to loop as it takes
     * considerably longer for the traces to get written to Cassandra or ElasticSearch than it does to create them.
     *
     * @throws IOException
     */
    @Test
    public void testCountTraces()  throws Exception {
        int actualTraceCount = 0;
        if (SPAN_STORAGE_TYPE.equals("cassandra")) {
            actualTraceCount = validateCassandraTraces(EXPECTED_TRACES);
        } else {
            actualTraceCount = validateElasticSearchTraces(EXPECTED_TRACES);
        }
        Files.write(Paths.get("traceCount.txt"), Long.toString(actualTraceCount).getBytes(), StandardOpenOption.CREATE);
        assertEquals("Did not find expected number of traces", EXPECTED_TRACES, actualTraceCount);
    }

    private int validateCassandraTraces(int expectedTraceCount) throws Exception {
        Session cassandraSession = getCassandraSession();
        int previousTraceCount = -1;
        int actualTraceCount = countTracesInCassandra(cassandraSession);
        int startTraceCount = actualTraceCount;
        int iterations = 0;
        while (actualTraceCount < expectedTraceCount && previousTraceCount < actualTraceCount) {
            logger.info("FOUND " + actualTraceCount + " traces in Cassandra");
            Thread.sleep(5000);
            previousTraceCount = actualTraceCount;
            actualTraceCount = countTracesInCassandra(cassandraSession);
            iterations++;
        }
        logger.info("FOUND " + actualTraceCount + " traces in Cassandra after " + iterations + " iterations, starting with " + startTraceCount);

        return actualTraceCount;
    }

    private Session getCassandraSession() {
        Cluster.Builder builder = Cluster.builder();
        builder.addContactPoint(CLUSTER_IP);
        Cluster cluster = builder.build();
        Session session = cluster.connect(KEYSPACE_NAME);

        return session;
    }

    /**
     * NOTE: select count(*) in Cassandra will often time out as it considers that an inefficient operation.  It will
     * however, happily permit us to do select *, so that is the hackerific workaround here.
     *
     * @param session
     * @return
     */
    private int countTracesInCassandra(Session session) {
        ResultSet result = session.execute("select * from traces");
        RowCountingConsumer consumer = new RowCountingConsumer();
        result.iterator()
                .forEachRemaining(consumer);
        int totalTraceCount = consumer.getRowCount();

        return totalTraceCount;
    }

    /**
     * It can take a while for traces to actually get written to storage, so both this and the Cassandra validation
     * method loop until they either find the expected number of traces, or the count returned ceases to increase
     *
     * @param expectedTraceCount
     * @return
     * @throws Exception
     */
    private int validateElasticSearchTraces(int expectedTraceCount) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = now.format(formatter);
        String targetUrlString = "/jaeger-span-" + formattedDate + "/_count";
        logger.info("Using ElasticSearch URL : [" + targetUrlString + "]" );

        RestClient restClient = getESRestClient();

        int previousTraceCount = -1;
        int actualTraceCount = getElasticSearchTraceCount(restClient, targetUrlString);
        int startTraceCount = actualTraceCount;
        int iterations = 0;
        /// TODO this is a guess that doesn't work very well; fix it
        long sleepDelay = Math.max(5, expectedTraceCount / 100000);   // delay 1 second for every 100,000 traces
        logger.info("Setting SLEEP DELAY " + sleepDelay + " seconds");
        while (actualTraceCount < expectedTraceCount && previousTraceCount < actualTraceCount) {
            logger.info("FOUND " + actualTraceCount + " traces in ElasticSearch");
            TimeUnit.SECONDS.sleep(sleepDelay);
            previousTraceCount = actualTraceCount;
            actualTraceCount = getElasticSearchTraceCount(restClient, targetUrlString);
            iterations++;
        }

        logger.info("FOUND " + actualTraceCount + " traces in ElasticSearch after " + iterations + " iterations, starting with " + startTraceCount);

        return actualTraceCount;
    }

    private RestClient getESRestClient() {
        return RestClient.builder(
                new HttpHost(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT, "http"),
                new HttpHost(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT +1, "http"))
                .build();
    }

    private int getElasticSearchTraceCount(RestClient restClient, String targetUrlString) throws Exception {
        Response response = restClient.performRequest("GET", targetUrlString);
        String responseBody = EntityUtils.toString(response.getEntity());
        ObjectMapper jsonObjectMapper = new ObjectMapper();
        JsonNode jsonPayload = jsonObjectMapper.readTree(responseBody);
        JsonNode count = jsonPayload.get("count");
        int traceCount = count.asInt();

        return traceCount;
    }


    @Ignore
    @Test
    public void testListKeyspaces() {
        Cluster.Builder builder = Cluster.builder();
        builder.addContactPoint(CLUSTER_IP);
        Cluster cluster = builder.build();

        listKeyspaces(cluster);
    }

    private void listKeyspaces(Cluster cluster) {
        List<KeyspaceMetadata> keyspaces = cluster.getMetadata().getKeyspaces();
        for (KeyspaceMetadata keyspace : keyspaces) {
            logger.info(">>>> KEYSPACE: " + keyspace.getName());
        }
    }
}

class RowCountingConsumer implements Consumer<Row> {
    AtomicInteger rowCount = new AtomicInteger(0);

    @Override
    public void accept(Row r) {
        rowCount.getAndIncrement();
    }

    public int getRowCount() {
        return rowCount.get();
    }
}
