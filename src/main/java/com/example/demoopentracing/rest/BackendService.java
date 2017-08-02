package com.example.demoopentracing.rest;

import io.opentracing.ActiveSpan;

import javax.inject.Inject;
import java.util.Random;

public class BackendService {

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
        tracer.activeSpan().setTag("anotherAction", "data");
    }
}
