package com.example.demoopentracing.rest;

import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.samplers.Sampler;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UdpSender;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@WebListener
public class TracingContextListener implements ServletContextListener {
    private static Map<String, String> envs = System.getenv();

    private static Integer JAEGER_FLUSH_INTERVAL = new Integer(envs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
    private static Integer JAEGER_MAX_PACKET_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));
    private static Integer JAEGER_MAX_QUEUE_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_QUEUE_SIZE", "50"));
    private static Double JAEGER_SAMPLING_RATE = new Double(envs.getOrDefault("JAEGER_SAMPLING_RATE", "1.0"));
    private static Integer JAEGER_UDP_PORT = new Integer(envs.getOrDefault("JAEGER_UDP_PORT", "5775"));
    // FIXME hardcoded jaeger-agent-host is a hack
    private static String JAEGER_AGENT_HOST = envs.getOrDefault("JAEGER_AGENT_HOST", "jaeger-agent.jaeger-performance.svc");
    private static final String TRACER_TYPE = envs.getOrDefault("TRACER_TYPE", "jaeger");
    private static final String TEST_SERVICE_NAME = envs.getOrDefault("TEST_SERVICE_NAME", "wildfly-swarm-opentracing-demo");
    private static Logger logger = Logger.getLogger(TracingContextListener.class.getName());

    @Inject
    private io.opentracing.Tracer tracer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        GlobalTracer.register(tracer);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}

    @Produces
    @Singleton
    public static io.opentracing.Tracer jaegerTracer() {
        Tracer tracer;

        logger.warning("****** HACK HACK HACK ********");
        logger.warning("******* REMOVE THIS **********");
        List<String> evNames = new ArrayList<>(envs.keySet());
        Collections.sort(evNames);
        for (String evName : evNames) {
            String value = envs.get(evName);
            logger.info(">>> " + evName + ": " + value);
        }

        if (TRACER_TYPE.equalsIgnoreCase("jaeger")) {
            logger.info("Using JAEGER tracer using host [" + JAEGER_AGENT_HOST + "] port [" + JAEGER_UDP_PORT +
                    "] Service Name " + TEST_SERVICE_NAME + " Sampling rate " + JAEGER_SAMPLING_RATE);

            Sender sender = new UdpSender(JAEGER_AGENT_HOST, JAEGER_UDP_PORT, JAEGER_MAX_PACKET_SIZE);
            Metrics metrics = new Metrics(new StatsFactoryImpl(new NullStatsReporter()));
            Reporter reporter = new RemoteReporter(sender, JAEGER_FLUSH_INTERVAL, JAEGER_MAX_QUEUE_SIZE, metrics);
            Sampler sampler = new ProbabilisticSampler(JAEGER_SAMPLING_RATE);
            tracer = new com.uber.jaeger.Tracer.Builder(TEST_SERVICE_NAME, reporter, sampler)
                    .build();
        } else {
            logger.info("Using NOOP Tracer");
            tracer = NoopTracerFactory.create();
        }

        return tracer;
    }
}