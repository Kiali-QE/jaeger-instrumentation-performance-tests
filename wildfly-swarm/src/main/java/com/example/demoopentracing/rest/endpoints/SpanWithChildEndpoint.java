package com.example.demoopentracing.rest.endpoints;

import com.example.demoopentracing.rest.util.BackendService;

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
	private static final Integer SLEEP_INTERVAL =
			Integer.parseInt(System.getenv().getOrDefault("SLEEP_INTERVAL", "1"));

	@Inject
	private BackendService backendService;

	@GET
	@Produces("text/plain")
	public Response doGet() throws InterruptedException {
		Thread.sleep(SLEEP_INTERVAL);
		backendService.action();
		return Response.ok("Hello from /spanWithChild " + new Date()).build();
	}
}