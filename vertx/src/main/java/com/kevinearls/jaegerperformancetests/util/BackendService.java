package com.kevinearls.jaegerperformancetests.util;

import io.opentracing.ActiveSpan;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.vertx.ext.web.TracingHandler;
import io.vertx.ext.web.RoutingContext;

import java.util.logging.Logger;

public class BackendService {
    public static Logger logger = Logger.getLogger(BackendService.class.getName());
    private TracingHandler tracingHandler;
    private Tracer tracer;

    public BackendService(Tracer tracer, TracingHandler tracingHandler) {
        this.tracingHandler = tracingHandler;
        this.tracer = tracer;
    }

    public void action(RoutingContext routingContext) throws InterruptedException {
        SpanContext spanContext = tracingHandler.serverSpanContext(routingContext);
        try (ActiveSpan span = tracer.buildSpan("action").asChildOf(spanContext).startActive()) {
            anotherAction();
        }
    }

    private void anotherAction() {
        ActiveSpan activeSpan = tracer.activeSpan();
        if (activeSpan != null) {
            activeSpan.setTag("anotherAction", "data");
        } else {
            logger.info("tracer.activeSpan returned null");
        }
    }
}
