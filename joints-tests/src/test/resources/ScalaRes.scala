import javax.ws.rs.core.MediaType._
import javax.ws.rs.{GET, Path, Produces}

@Path("/scala")
class ScalaRes {
    @GET
    @Produces(Array(TEXT_PLAIN))
    @Path("helloworld")
    def helloworld(): String = {
        return "Hello to Script World for Scala!!!" + new java.util.Date();
    }

    @GET
    @Produces(Array(TEXT_PLAIN))
    @Path("bye")
    def helloworld(): String = {
        return "Scala scripting resource, bye!!!" + new java.util.Date();
    }
}