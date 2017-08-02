package com.example.demoopentracing.rest;

import io.opentracing.ActiveSpan;

import javax.inject.Inject;
import java.util.Random;
import java.util.logging.Logger;

public class BackendService {
    public static Logger logger = Logger.getLogger(BackendService.class.getName());

    @Inject
    private io.opentracing.Tracer tracer;

    public String action() throws InterruptedException {
        int random = new Random().nextInt(200);

        try (ActiveSpan span = tracer.buildSpan("action").startActive()) {
            anotherAction();
            Thread.sleep(random);
        }

        return String.valueOf(random);
    }

    private void anotherAction() {
        ActiveSpan activeSpan = tracer.activeSpan();
        if (activeSpan == null) {
            logger.warning("tracer.activeSpan returned null!");
        } else {
            activeSpan.setTag("anotherAction", "data");
        }
    }
}
