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

import com.uber.jaeger.Tracer;
import com.uber.jaeger.metrics.Counter;
import com.uber.jaeger.metrics.Metrics;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.logging.Logger;

@ApplicationScoped
@Path("/closeTracer")
public class CloseTracer {
    private static Logger logger = Logger.getLogger(CloseTracer.class.getName());

    @Inject
    private io.opentracing.Tracer tracer;

    @GET
    @Produces("text/plain")
    public Response doGet() throws InterruptedException {
        logger.info(">>>> Closing tracer");
        com.uber.jaeger.Tracer jaegerTracer = (com.uber.jaeger.Tracer) tracer;
        jaegerTracer.close();

        return Response.ok("Closed tracer at " + new Date()).build();
    }
}
