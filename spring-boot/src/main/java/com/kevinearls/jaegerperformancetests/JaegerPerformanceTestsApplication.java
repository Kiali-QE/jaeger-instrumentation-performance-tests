package com.kevinearls.jaegerperformancetests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import com.uber.jaeger.Configuration;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class JaegerPerformanceTestsApplication {
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}

	@Bean
	public io.opentracing.Tracer jaegerTracer() {
		Configuration.SamplerConfiguration samplerConfiguration = new Configuration.SamplerConfiguration(ProbabilisticSampler.TYPE, 1);
		Configuration.ReporterConfiguration reporterConfiguration = new Configuration.ReporterConfiguration();
		Configuration jaegerConfiguration = new Configuration("spring-boot", samplerConfiguration, reporterConfiguration);

		io.opentracing.Tracer jaegerTracer = jaegerConfiguration.getTracer();
		return jaegerTracer;
	}

	public static void main(String[] args) {
		SpringApplication.run(JaegerPerformanceTestsApplication.class, args);
	}
}
