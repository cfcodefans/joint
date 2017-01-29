package tests;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

@Path("tests/simple")
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

    @Path("mul")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public int postMultiply(@QueryParam("a") int a, @QueryParam("b") int b) {
        return a * b;
    }

    @Path("div")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public int postDivide(@QueryParam("a") int a, @QueryParam("b") int b) {
        return a / b;
    }

    @Path("int-array")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public int[] getIntArray() {
        return new int[]{3, 1, 4, 1, 5, 9};
    }

    @Path("int-list")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public List<Integer> getIntList() {
        return Arrays.asList(new Integer[]{3, 1, 4, 1, 5, 9});
    }
}
