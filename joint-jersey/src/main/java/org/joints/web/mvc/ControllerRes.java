package org.joints.web.mvc;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.mvc.spi.ResolvedViewable;
import org.joints.commons.MiscUtils;
import org.joints.rest.RestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/")
// @RequestScoped
public class ControllerRes {
	private static final Logger log = LogManager.getLogger(ControllerRes.class);
	private static final String RES_PATH = "/";

	public ControllerRes() {
		log.debug(MiscUtils.invocationInfo());
	}

	private @Context HttpServletRequest req;
	private @Context HttpServletResponse resp;

	protected @Context
    ThreadLocal<HttpServletRequest> reqInvoker;
	protected @Context
    ThreadLocal<HttpServletResponse> respInvoker;

	@Path("{sub_path: .*}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response post(final @Context UriInfo ui, final @PathParam("sub_path") String subPathStr, MultivaluedMap<String, String> paramsMap) {
		log.info(String.format("path: %s, subPathStr: %s", ui.getPath(), subPathStr));
		final HttpServletRequest _req = req; // reqInvoker.get();
		final HttpServletResponse _resp = resp; // respInvoker.get();
		final String basePath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(RES_PATH, ControllerHelper.getBasePath(_req)));

		_req.setAttribute("paramsMap", RestUtils.extractParams(paramsMap));

		ResolvedViewable<String> viewable = ProcessorFactory.getViewable(_resp, _req, basePath, subPathStr, this);
		return Response.ok(viewable).type(viewable.getMediaType()).build();
	}

	@Path("{sub_path: .*}")
	@GET
	public Response get(final @Context UriInfo ui, final @PathParam("sub_path") String subPathStr) {
		log.info(String.format("path: %s, subPathStr: %s", ui.getPath(), subPathStr));
		final HttpServletRequest _req = req; // reqInvoker.get();
		final HttpServletResponse _resp = resp; // respInvoker.get();
		final String basePath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(RES_PATH, ControllerHelper.getBasePath(_req)));

		_req.setAttribute("paramsMap", RestUtils.extractParams(ui.getQueryParameters()));

		ResolvedViewable<String> viewable = ProcessorFactory.getViewable(_resp, _req, basePath, subPathStr, this);
		return Response.ok(viewable).type(viewable.getMediaType()).build();
	}
}
