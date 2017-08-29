package com.kevinearls.jaegerperformancetests;

import com.kevinearls.jaegerperformancetests.util.BackendService;
import io.opentracing.Tracer;
import io.opentracing.contrib.vertx.ext.web.TracingHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Date;
import java.util.logging.Logger;

import static com.kevinearls.jaegerperformancetests.util.TracerUtil.jaegerTracer;

public class JaegerPerformanceTestsVerticle extends AbstractVerticle {
    private static final Integer SLEEP_INTERVAL = Integer.parseInt(System.getenv().getOrDefault("SLEEP_INTERVAL", "10"));
    private static Logger logger = Logger.getLogger(JaegerPerformanceTestsVerticle.class.getName());
    private BackendService backendService;

    @Override
    public void start() {
        Tracer tracer = jaegerTracer();

        TracingHandler tracingHandler = new TracingHandler(tracer);
        backendService = new BackendService(tracer);

        Router router = Router.router(vertx);
        router.get("/").handler(this::singleSpan);
        router.get("/singleSpan").handler(this::singleSpan);
        router.get("/spanWithChild").handler(this::spanWithChild);

        router.route()
                .order(-1).handler(tracingHandler)
                .failureHandler(tracingHandler);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080);
    }

    private void singleSpan(RoutingContext routingContext) {
        String message = "Hello from /singleSpan at " + new Date();

        HttpServerResponse httpServerResponse = routingContext.response();
        httpServerResponse.putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaders.TEXT_HTML);
        httpServerResponse.end(message);
    }

    private void spanWithChild(RoutingContext routingContext) {
        try {
            Thread.sleep(SLEEP_INTERVAL);
            backendService.action();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String message = "Hello from /spanWithChild " + new Date();
        HttpServerResponse httpServerResponse = routingContext.response();
        httpServerResponse.putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaders.TEXT_HTML);
        httpServerResponse.end(message);
    }
}
