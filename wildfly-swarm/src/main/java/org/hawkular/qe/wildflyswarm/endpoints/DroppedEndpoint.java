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
package org.hawkular.qe.wildflyswarm.endpoints;

import com.uber.jaeger.metrics.Counter;
import com.uber.jaeger.metrics.Metrics;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Date;

@ApplicationScoped
@Path("/dropped")
public class DroppedEndpoint {

    @Inject
    private io.opentracing.Tracer tracer;

    @GET
    @Produces("text/plain")
    public Response doGet() throws InterruptedException {
        com.uber.jaeger.Tracer jaegerTracer = (com.uber.jaeger.Tracer) tracer;
        Metrics metrics = jaegerTracer.getMetrics();


        //metrics.reporterQueueLength.
        Counter reporterDropped = metrics.reporterDropped;
        System.out.println(">>>> Counter is a " + reporterDropped.getClass().getCanonicalName());
        System.out.println("METRICS " + metrics);
        // com.uber.jaeger.metrics.StatsFactoryImpl

        Counter spansSampled = metrics.spansSampled;
        Counter spansNotSampled = metrics.spansNotSampled;

        String results = "Dropped [" + reporterDropped.toString() + "] Sampled [" + spansSampled.toString() + "] NOT Sampled [" + spansNotSampled.toString() + "]";

        return Response.ok(results + " at " + new Date()).build();
    }
}
