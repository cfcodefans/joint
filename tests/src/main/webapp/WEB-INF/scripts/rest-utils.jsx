var Endpoint = Java.type("org.glassfish.jersey.server.internal.process.Endpoint");

function _endPoint(func) {
    var ResourceType = Java.extend(Endpoint, {
        apply:func
    });
    return new ResourceType();
}
