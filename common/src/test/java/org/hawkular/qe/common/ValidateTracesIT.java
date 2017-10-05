package org.hawkular.qe.common;

import com.datastax.driver.core.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        ResultSet result = session.execute(QUERY);
        Row one = result.one();
        long traceCount = one.getLong(0);
        logger.info(">>>>> TRACE COUNT: " + traceCount);

        // FIXME At some point we need to pass in the expected trace count.  In the short term write it to a file.  Then
        // We can have the JenkinsFIle add it to the description of the job
        Files.write(Paths.get("traceCount.txt"), Long.toString(traceCount).getBytes(), StandardOpenOption.CREATE);
    }

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
