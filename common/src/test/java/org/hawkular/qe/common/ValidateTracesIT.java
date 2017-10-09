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
    private static String QUERY;

    private static Logger logger = Logger.getLogger(ValidateTracesIT.class.getName());

    private Cluster cluster;

    @BeforeClass
    public static void beforeClass() {
        CLUSTER_IP = System.getProperty("cluster.ip", "cassandra");
        KEYSPACE_NAME = System.getProperty("keyspace.name", "jaeger_v1_dc1");
        QUERY = System.getProperty("query", "SELECT COUNT(*) FROM traces");
        logger.info("Running with custer.ip [" + CLUSTER_IP + "] Keyspace [" + KEYSPACE_NAME + "] Query [" + QUERY + "]");
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

    @Test
    public void testCountTraces()  throws IOException {
        Session session = cluster.connect(KEYSPACE_NAME);
        long minStartTime = getAggregateValue(session, "select min(start_time) from traces ALLOW FILTERING");
        long maxtartTime = getAggregateValue(session, "select max(start_time) from traces ALLOW FILTERING");
        logger.info("MIN " + minStartTime + " MAX " + maxtartTime);

        long totalTraceCount = 0;
        long startTime = minStartTime;
        while (startTime <= maxtartTime) {
            String query = "select count(*) from traces where  start_time=" + startTime + " ALLOW FILTERING;";
            long traceCountForMs = getAggregateValue(session, query);
            logger.info("AT " + startTime + " got " + traceCountForMs);
            startTime +=1000;  // TODO confirm we only store down to ms
            totalTraceCount += traceCountForMs;
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
        Row one = result.one();
        BigInteger stupid = one.getVarint(0);
        logger.info("Query [" + query + "] returned [" + stupid.longValue() + "]");
        return stupid.longValue();
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
