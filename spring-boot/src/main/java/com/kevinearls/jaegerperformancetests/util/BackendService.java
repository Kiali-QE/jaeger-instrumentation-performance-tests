package com.kevinearls.jaegerperformancetests.util;

import io.opentracing.ActiveSpan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class BackendService {
    private static final Integer SLEEP_INTERVAL = Integer.parseInt(System.getenv().getOrDefault("SLEEP_INTERVAL", "10"));
    public static Logger logger = Logger.getLogger(BackendService.class.getName());

    @Autowired
    private io.opentracing.Tracer tracer;

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
            logger.fine("tracer.activeSpan returned null");
        }
    }
}
