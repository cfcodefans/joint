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
}

classOf[ScalaRes]

// /class ResGen extends IResourceGenerator {
//    def apply(app: Application): java.util.Set[Resource] = {
//        return new java.util.HashSet[Resource](
//            Set(Resource.from(classOf[ScalaRes])).asJavaCollection
//        )
//    }
//}
//new ResGen