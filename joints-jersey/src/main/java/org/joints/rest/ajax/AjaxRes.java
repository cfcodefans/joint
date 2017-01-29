package org.joints.rest.ajax;

import org.apache.commons.lang3.StringUtils;
import org.joints.commons.Jsons;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("ajax")
@Singleton
public class AjaxRes {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
    @Path("/res.js")
	public Response getJSProxy(@Context UriInfo uriInfo, @QueryParam("appName") String name) {
		List<AjaxResMetadata> proxyList = AjaxResContext.getInstance(name).getProxyList();
		proxyList.forEach(armd -> armd.setBaseUrl(StringUtils.removeEnd(uriInfo.getAbsolutePath().getPath(), "ajax")));
		return Response.ok(Jsons.toString(proxyList.toArray(new AjaxResMetadata[0]))).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/res.ts")
	public Response getTypeScriptProxy(@Context UriInfo uriInfo, @QueryParam("appName") String name) {
		List<AjaxResMetadata> proxyList = AjaxResContext.getInstance(name).getProxyList();
		proxyList.forEach(armd -> armd.setBaseUrl(StringUtils.removeEnd(uriInfo.getAbsolutePath().getPath(), "ajax")));
		return Response.ok(Jsons.toString(proxyList.toArray(new AjaxResMetadata[0]))).build();
	}
}
