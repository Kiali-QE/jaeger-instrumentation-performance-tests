package com.example.demoopentracing.rest.endpoints;


import javax.faces.bean.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Date;

@ApplicationScoped
@Path("/singleSpan")
public class SingleSpanEndpoint {
    private static final Integer SLEEP_INTERVAL =
            Integer.parseInt(System.getenv().getOrDefault("SLEEP_INTERVAL", "1"));

    @GET
    @Produces("text/plain")
    public Response doGet() throws InterruptedException {
        Thread.sleep(SLEEP_INTERVAL);
        return Response.ok("Hello from /singleSpan at " + new Date()).build();
    }
}
