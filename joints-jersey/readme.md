
## Joints Jersey
This is some naive attempt for different alternatives for Java EE web application.
It is still based on Java EE tech stack
### Ajax Bridge 
This installs a listener into Jersey Restful framework so that it detects restful api's metadata
such as resources paths, parameters, and http methods
For example in joints-tests, 
```java
package tests;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("tests")
public class TestsRes {
    @Path("add/{a}/{b}")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public int getAdd(@PathParam("a") int a, @PathParam("b") int b) {  return a + b;  }

    @Path("add/{a}/{b}")
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public int postAdd(@PathParam("a") int a, @PathParam("b") int b) { return a + b; }//....
}
```
from this dull rest api for testing
The code in joints-jersey can extract the meta data presented in json and expose it like this, (configured in joints-tests)
http://localhost:8080/rest/v1/ajax?appName=rest-ajax
```json
[ {
  "name" : "TestsRes",
  "children" : [ {
    "children" : [ ],
    "methods" : [ {
      "jaxrsType" : "RESOURCE_METHOD","name" : "getAdd","returnType" : "int", "httpMethod" : "GET",
      "params" : [ {"source" : "PATH","sourceName" : "a","rawType" : "int","type" : "int" }
      , {"source" : "PATH","sourceName" : "b","rawType" : "int","type" : "int"} ],
      "isArray" : false,
      "produceMediaTypes" : [ "text/plain" ],
      "consumedMediaTypes" : [ ]
    }, {
      "jaxrsType" : "RESOURCE_METHOD",
      "name" : "postAdd", "returnType" : "int", "httpMethod" : "POST",
      "params" : [ {"source" : "PATH", "sourceName" : "a", "rawType" : "int", "type" : "int"}
      , {"source" : "PATH", "sourceName" : "b", "rawType" : "int", "type" : "int"} ],
      "isArray" : false,
      "produceMediaTypes" : [ "text/plain" ],
      "consumedMediaTypes" : [ ]
    } ],
    "url" : "/rest/v1/tests/add/{a}/{b}"
  }//...
```
and these restful api interfaces are wrapped by rest.js so they could be invoked in html pages in browser
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>For Tests</title>
</head>
<body>
<script src="../js/jquery-2.1.1.js"></script>
<script src="../js/rest.js" ></script>
<script type="text/javascript">
    console.info(RS.ctx.TestsRes.getAdd //mapped to TestsRes.getAdd
        .with_a(1) // mapped to parameter a
        .with_b(2) // mapped to parameter b
        .call());  // above metadata is wrapped with jquery ajax call
</script>
</body>
</html>
```
in chrom console, the result of invocation is shown like
```json
Object {readyState: 4, responseText: "3", status: 200, statusText: "OK"}
```

### Script Resource
org.joints.rest.script.ScriptResLoader as a jersey restful resource configuraion, it sets up a file system monitor watching
certain folder; when some script files are placed or modified, it will reload Jersey container to reload 
the changed restful api interfaces.
Example of Nashorn Javascript (Javascript is such bad example that Jersey could not properly struct the restful api interface 
since Javascript has not static type and annotations)
```javascript
var Resource = Java.type("org.glassfish.jersey.server.model.Resource");
var ResourceMethod = Java.type("org.glassfish.jersey.server.model.ResourceMethod");

var ContainerRequestContext = Java.type("javax.ws.rs.container.ContainerRequestContext");
var MediaType = Java.type("javax.ws.rs.core.MediaType");
var IResourceGenerator = Java.type("org.joints.rest.script.ScriptResLoader.IResourceGenerator");
var Arrays = Java.type(java.util.Arrays.class.getName());
var HashSet = Java.type(java.util.HashSet.class.getName());
load(current_path + "/rest-utils.jsx");
function apply(resourceConfig) { //ScriptResLoader
    var resourceBuilder = Resource.builder();
    resourceBuilder.path("helloworld");

    var methodBuilder = resourceBuilder.addMethod("GET");
    methodBuilder
        .produces(MediaType.TEXT_PLAIN_TYPE)
        .handledBy(
           _endPoint(function(containerRequestContext) {
                    return this.toString() + "\n" + containerRequestContext;
            }));
    var resource = resourceBuilder.build();
    return new HashSet(Arrays.asList(resource));
}
var ResGen = Java.extend(IResourceGenerator, {apply: apply});
new ResGen();
```

### Scala compiled Resource
Scala could be better as it has compiler api to compile Scala code into class, so the file monitor can load 
and compile scala source files into Jersey restful api interfaces.
such as 
```scala
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
```
