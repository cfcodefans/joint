var Inflector = Java.type("org.glassfish.jersey.process.Inflector");
var Resource = Java.type("org.glassfish.jersey.server.model.Resource");
var ResourceMethod = Java.type("org.glassfish.jersey.server.model.ResourceMethod");

var ContainerRequestContext = Java.type("javax.ws.rs.container.ContainerRequestContext");
var MediaType = Java.type("javax.ws.rs.core.MediaType");
var IResourceGenerator = Java.type("org.joints.rest.script.ScriptResLoader.IResourceGenerator");
var Arrays = Java.type(java.util.Arrays.class.getName());
var HashSet = Java.type(java.util.HashSet.class.getName());

function apply(resourceConfig) { //ScriptResLoader
    var resourceBuilder = Resource.builder();
    resourceBuilder.path("helloworld");

    var methodBuilder = resourceBuilder.addMethod("GET");
    methodBuilder
        .produces(MediaType.TEXT_PLAIN_TYPE)
        .handledBy(
            function(containerRequestContext) {
                    return "Hello Script World for Java!!! " + new java.util.Date() + "\n" + containerRequestContext;
            });

    var resource = resourceBuilder.build();
    print(resource);
    return new HashSet(Arrays.asList(resource));
}

var ResGen = Java.extend(IResourceGenerator, {apply: apply});
var resGen = new ResGen();