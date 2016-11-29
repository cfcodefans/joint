
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.{Application, MediaType, Response}

import org.glassfish.jersey.server.model.{Resource, ResourceMethod}
import org.joints.rest.script.ScriptResLoader.IResourceGenerator

import scala.collection.JavaConverters._

class ScalaResGenerator extends IResourceGenerator {
    override def apply(app: Application): java.util.Set[Resource] = {
        val rb: Resource.Builder = Resource.builder("scala")

        {
            val mb: ResourceMethod.Builder = rb.addMethod("GET")
            mb.produces(MediaType.TEXT_PLAIN_TYPE)
                .handledBy((data: ContainerRequestContext) => {
                    Response.ok(s"data: $data").build()
                })
            val res: Resource = rb.build()
            return Set(res).asJava
        }
    }
}

new ScalaResGenerator