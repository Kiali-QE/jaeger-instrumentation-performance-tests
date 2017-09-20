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
package com.kevinearls.jaegerperformancetests;

import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.Sampler;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UdpSender;
import io.opentracing.NoopTracerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.logging.Logger;

@SpringBootApplication
public class JaegerPerformanceTestsApplication {
    private static Map<String, String> envs = System.getenv();

    private static Integer JAEGER_FLUSH_INTERVAL = new Integer(envs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
    private static Integer JAEGER_MAX_PACKET_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));
    private static Integer JAEGER_MAX_QUEUE_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_QUEUE_SIZE", "50"));
    private static Double JAEGER_SAMPLING_RATE = new Double(envs.getOrDefault("JAEGER_SAMPLING_RATE", "1.0"));
    private static Integer JAEGER_UDP_PORT = new Integer(envs.getOrDefault("JAEGER_UDP_PORT", "5775"));
    private static String JAEGER_AGENT_HOST = envs.getOrDefault("JAEGER_AGENT_HOST", "localhost");
    private static final String TRACER_TYPE = envs.getOrDefault("TRACER_TYPE", "jaeger");
    private static  String TEST_SERVICE_NAME = envs.getOrDefault("TEST_SERVICE_NAME", "spring-boot-opentracing-demo");

    private static Logger logger = Logger.getLogger(JaegerPerformanceTestsApplication.class.getName());

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}

	@Bean
	public io.opentracing.Tracer tracer() {
        io.opentracing.Tracer tracer;

        if (TRACER_TYPE.equalsIgnoreCase("jaeger")) {
            logger.info("Using JAEGER tracer using host [" + JAEGER_AGENT_HOST + "] port [" + JAEGER_UDP_PORT
                    + "] Service Name " + TEST_SERVICE_NAME + " Sampling rate " + JAEGER_SAMPLING_RATE);

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

	public static void main(String[] args) {
		SpringApplication.run(JaegerPerformanceTestsApplication.class, args);
	}
}
