package com.example.demoopentracing.rest;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.Configuration.ReporterConfiguration;
import com.uber.jaeger.Configuration.SamplerConfiguration;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Map;
import java.util.logging.Logger;

@WebListener
public class TracingContextListener implements ServletContextListener {
    private static Map<String, String> envs = System.getenv();
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

        if (TRACER_TYPE.equalsIgnoreCase("jaeger")) {
            logger.info("Using JAEGER tracer, Service Name " + TEST_SERVICE_NAME);

            SamplerConfiguration samplerConfiguration = new SamplerConfiguration(ProbabilisticSampler.TYPE, 1);
            ReporterConfiguration reporterConfiguration = new ReporterConfiguration();
            Configuration tracerConfiguration = new Configuration(TEST_SERVICE_NAME, samplerConfiguration, reporterConfiguration);

            tracer = tracerConfiguration.getTracer();
        } else {
            logger.info("Using NOOP Tracer");
            tracer = NoopTracerFactory.create();
        }

        return tracer;
    }
}