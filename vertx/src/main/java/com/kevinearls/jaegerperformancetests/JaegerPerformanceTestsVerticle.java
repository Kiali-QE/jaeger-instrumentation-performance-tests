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
    private static Logger logger = Logger.getLogger(JaegerPerformanceTestsVerticle.class.getName());
    private BackendService backendService;

    @Override
    public void start() {
        Tracer tracer = jaegerTracer();
        TracingHandler tracingHandler = new TracingHandler(tracer);
        backendService = new BackendService(tracer, tracingHandler);

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
            backendService.action(routingContext);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String message = "Hello from /spanWithChild " + new Date();
        HttpServerResponse httpServerResponse = routingContext.response();
        httpServerResponse.putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaders.TEXT_HTML);
        httpServerResponse.end(message);
    }
}
