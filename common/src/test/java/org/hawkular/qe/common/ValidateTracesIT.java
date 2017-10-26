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
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class ValidateTracesIT {
    private static Map<String, String> envs = System.getenv();

    private static final Integer ITERATIONS = new Integer(envs.getOrDefault("ITERATIONS", "100"));
    private static final Integer JMETER_CLIENT_COUNT = new Integer(envs.getOrDefault("JMETER_CLIENT_COUNT", "100"));
    private static int EXPECTED_TRACES = ITERATIONS * JMETER_CLIENT_COUNT * 3;

    private static String CLUSTER_IP;
    private static String KEYSPACE_NAME;
    private Cluster cluster;

    private static Logger logger = Logger.getLogger(ValidateTracesIT.class.getName());

    @BeforeClass
    public static void beforeClass() {
        CLUSTER_IP = System.getProperty("cluster.ip", "cassandra");
        KEYSPACE_NAME = System.getProperty("keyspace.name", "jaeger_v1_dc1");
        logger.info("Running with custer.ip [" + CLUSTER_IP + "] Keyspace [" + KEYSPACE_NAME + "]");
    }

    @Before
    public void setup() {
        Cluster.Builder builder = Cluster.builder();
        builder.addContactPoint(CLUSTER_IP);
        cluster = builder.build();
    }

    @After
    public void tearDown() {
       cluster.close();
    }

    /**
     * Try to confirm that the expected number of traces was created.  At this time we need to loop as it takes
     * considerably longer for the traces to get written to Cassandra than it does to create them.
     *
     * NOTE: select count(*) in Cassandra will often time out as it considers that an inefficient operation.  It will
     * however, happily permit us to do select *, so that is the hackerific workaround here.
     * @throws IOException
     */
    @Test
    public void testCountTraces()  throws Exception {
        Session cassandraSession = getCassandraSession();
        int previousTraceCount = -1;
        int actualTraceCount = countTracesInCassandra(cassandraSession);
        int startTraceCount = actualTraceCount;
        int iterations = 0;
        while (actualTraceCount < EXPECTED_TRACES && previousTraceCount < actualTraceCount) {
            logger.info("FOUND " + actualTraceCount + " traces in Cassandra");
            Thread.sleep(5000);
            previousTraceCount = actualTraceCount;
            actualTraceCount = countTracesInCassandra(cassandraSession);
            iterations++;
        }

        // TODO temporary, remove this.  Just trying to verify if the Agent is really dropping things
        TimeUnit.SECONDS.sleep(30);
        actualTraceCount = countTracesInCassandra(cassandraSession);

        logger.info("FOUND " + actualTraceCount + " traces in Cassandra after " + iterations + " iterations, starting with " + startTraceCount);
        Files.write(Paths.get("traceCount.txt"), Long.toString(actualTraceCount).getBytes(), StandardOpenOption.CREATE);
        assertEquals("Did not find expected number of traces", EXPECTED_TRACES, actualTraceCount);
    }

    private Session getCassandraSession() {
        Cluster.Builder builder = Cluster.builder();
        builder.addContactPoint(CLUSTER_IP);
        Cluster cluster = builder.build();
        Session session = cluster.connect(KEYSPACE_NAME);

        return session;
    }

    private int countTracesInCassandra(Session session) {
        ResultSet result = session.execute("select * from traces");
        RowCountingConsumer consumer = new RowCountingConsumer();
        result.iterator()
                .forEachRemaining(consumer);
        int totalTraceCount = consumer.getRowCount();

        return totalTraceCount;
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
