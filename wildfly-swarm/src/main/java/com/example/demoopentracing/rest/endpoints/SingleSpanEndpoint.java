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

    @GET
    @Produces("text/plain")
    public Response doGet() throws InterruptedException {
        return Response.ok("Hello from /singleSpan at " + new Date()).build();
    }
}
