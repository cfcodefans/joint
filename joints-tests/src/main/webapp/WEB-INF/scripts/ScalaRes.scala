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
    def bye(): String = {
        return "Scala scripting resource, bye!!!" + new java.util.Date();
    }

    @GET
    @Produces(Array(TEXT_PLAIN))
    @Path("do")
    def doSomething(): String = {
        return "I am working on my job!!!" + new java.util.Date();
    }

   @GET
   @Produces(Array(TEXT_PLAIN))
   @Path("no")
   def doNothing(): String = {
       return "I am NOT doing my job!!!" + new java.util.Date();
   }
}