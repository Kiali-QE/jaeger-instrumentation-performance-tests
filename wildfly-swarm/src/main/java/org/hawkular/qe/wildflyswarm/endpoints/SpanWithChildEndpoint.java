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

import org.hawkular.qe.wildflyswarm.util.BackendService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import java.util.Date;

@ApplicationScoped
@Path("/spanWithChild")
public class SpanWithChildEndpoint {

	@Inject
	private BackendService backendService;

	@GET
	@Produces("text/plain")
	public Response doGet() throws InterruptedException {
		backendService.action();
		return Response.ok("Hello from /spanWithChild " + new Date()).build();
	}
}