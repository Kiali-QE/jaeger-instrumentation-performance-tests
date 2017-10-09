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

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.*;

public class ValidateTracesIT {
    private static Map<String, String> envs = System.getenv();

    private static String CLUSTER_IP;
    private static String KEYSPACE_NAME;
    private static String QUERY;  // TODO remove?

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
     * NOTE: select count(*) in Cassandra will often time out as it considers that an inefficient operation.  In order to
     * avoid this we will get counts in one second chunks and report the sum
     * @throws IOException
     */
    @Test
    public void testCountTraces()  throws IOException {
        Session session = cluster.connect(KEYSPACE_NAME);
        long minStartTime = getAggregateValue(session, "select min(start_time) from traces ALLOW FILTERING");
        long maxStartTime = getAggregateValue(session, "select max(start_time) from traces ALLOW FILTERING");
        logger.info("MIN " + minStartTime + " MAX " + maxStartTime + " DIFF " + (maxStartTime - minStartTime)/1000);

        long totalTraceCount = 0;
        long startTime = minStartTime;
        long interval = 1_000_000L;  // Trace start_time is in microseconds, so this should get 1 sec at a time.
        while (startTime <= (maxStartTime + 1)) {
            long endTime = startTime + interval;
            String query = "select count(*) from traces where  start_time >" + startTime + " AND start_time < " + endTime + " ALLOW FILTERING;";
            long currentTraceCount = getAggregateValue(session, query);
            startTime +=interval;
            totalTraceCount += currentTraceCount;
        }

        logger.info(">>>>> TRACE COUNT: " + totalTraceCount);
        // FIXME At some point we need to pass in the expected trace count.  In the short term write it to a file.  Then
        // We can have the JenkinsFIle add it to the description of the job
        Files.write(Paths.get("traceCount.txt"), Long.toString(totalTraceCount).getBytes(), StandardOpenOption.CREATE);
    }

    /**
     * This method is to simplify aggregate queries.
     *
     * @param session The Cassandra session
     * @param query Text of the query
     * @return single long result of the query: COUNT, MAX, MIN, etc.
     */
    private long getAggregateValue(Session session, String query) {
        ResultSet result = session.execute(query);

        String firstRowValue = result.one().toString();
        // In a non-stupid world. Cassandra would be able to convert its own bigint type to a java BigInteger
        // without having to manually load codecs.  For now we will go with this hacky, but simpler solution
        // This will return "Row[nnnn]"  So we need to chop of Row[ and the tailing ]
        String start = firstRowValue.substring(4);
        String finalValue = start.substring(0, start.length() - 1);
        long aggregate = new Long(finalValue);

        logger.info("Query [" + query + "] returned [" + aggregate + "]");
        return aggregate;
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
