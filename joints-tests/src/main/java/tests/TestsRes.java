package tests;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("tests")
public class TestsRes {
    @Path("add/{a}/{b}")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public int getAdd(@PathParam("a") int a, @PathParam("b") int b) {
        return a + b;
    }

    @Path("add/{a}/{b}")
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public int postAdd(@PathParam("a") int a, @PathParam("b") int b) {
        return a + b;
    }

    @Path("multiply")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public int postMultiply(@QueryParam("a") int a, @QueryParam("b") int b) {
        return a * b;
    }
}
