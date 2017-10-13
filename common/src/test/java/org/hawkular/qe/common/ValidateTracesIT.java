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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ValidateTracesIT {
    private static Map<String, String> envs = System.getenv();

    private static String CLUSTER_IP;
    private static String KEYSPACE_NAME;

    private static Logger logger = Logger.getLogger(ValidateTracesIT.class.getName());

    private Cluster cluster;

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
     * At present this is not really a test, but will count the number of traces in Jaeger's Cassandra database and
     * then write the result to a text file, which the Jenkinsfile will then use to report the result.  In the future
     * the expected count could be passed in and the test could run against that.
     *
     * NOTE: select count(*) in Cassandra will often time out as it considers that an inefficient operation.  It will
     * however, happily permit us to do select *, so that is the hackerific workaround here.
     * @throws IOException
     */
    @Test
    public void testCountTraces()  throws IOException {
        Session session = cluster.connect(KEYSPACE_NAME);

        ResultSet result = session.execute("select * from traces");
        RowCountingConsumer consumer = new RowCountingConsumer();
        result.iterator()
                .forEachRemaining(consumer);
        int totalTraceCount = consumer.getRowCount();
        logger.info(">>>> GOT " + totalTraceCount + " rows");

        // FIXME At some point we need to pass in the expected trace count.  In the short term write it to a file.  Then
        // We can have the JenkinsFIle add it to the description of the job
        Files.write(Paths.get("traceCount.txt"), Long.toString(totalTraceCount).getBytes(), StandardOpenOption.CREATE);
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

    // TODO maintain separate counts based on operation name
    @Override
    public void accept(Row r) {
        rowCount.getAndIncrement();
    }

    public int getRowCount() {
        return rowCount.get();
    }
}
