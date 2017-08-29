package com.kevinearls.jaegerperformancetests.util;

import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;

import java.util.logging.Logger;

public class BackendService {
    private static final Integer SLEEP_INTERVAL = Integer.parseInt(System.getenv().getOrDefault("SLEEP_INTERVAL", "10"));
    public static Logger logger = Logger.getLogger(BackendService.class.getName());
    private Tracer tracer;

    public BackendService(Tracer tracer) {
        this.tracer = tracer;
    }

    public void action() throws InterruptedException {
        try (ActiveSpan span = tracer.buildSpan("action").startActive()) {
            anotherAction();
            Thread.sleep(SLEEP_INTERVAL);
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
