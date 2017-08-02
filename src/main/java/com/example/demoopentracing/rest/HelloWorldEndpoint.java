package com.example.demoopentracing.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import java.util.Date;

@ApplicationScoped
@Path("/hello")
public class HelloWorldEndpoint {

	@Inject
	private BackendService backendService;

	@GET
	@Produces("text/plain")
	public Response doGet() throws InterruptedException {
		String action = backendService.action();
		return Response.ok("Hello from WildFly Swarm! It is now " + new Date()).build();
	}
}