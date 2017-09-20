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
