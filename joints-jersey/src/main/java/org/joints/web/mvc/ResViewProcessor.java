package org.joints.web.mvc;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ResViewProcessor implements TemplateProcessor<String> {

	protected HttpServletRequest req;
	protected HttpServletResponse resp;
	protected MediaType mediaType;
	
	protected String basePathStr = "/";
	private static final Logger log = LogManager.getLogger(ResViewProcessor.class);
	
	public ResViewProcessor(final HttpServletRequest _req, 
							final HttpServletResponse _resp, 
							final String _basePathStr,
							final MediaType mediaType) {//, final BeanManager _beanMgr) {
		super();
		this.req = _req;
		this.resp = _resp;
		this.basePathStr = StringUtils.defaultIfBlank(_basePathStr, "/");
		this.mediaType = mediaType;
	}
	
	public ResViewProcessor(final HttpServletRequest _req, 
							final HttpServletResponse _resp) {//, final BeanManager _beanMgr) {
		super();
		this.req = _req;
		this.resp = _resp;
		this.basePathStr = ControllerHelper.getBasePath(_req);
		this.mediaType = MediaType.WILDCARD_TYPE;
	}
	
	@Override
	public String resolve(final String name, MediaType mediaType) {
		log.debug(name);
		return FilenameUtils.separatorsToUnix(FilenameUtils.concat(basePathStr, name));
//		return "/" + basePathStr + (basePathStr.endsWith("/") ? "" : "/") + name;
	}

	@Override
	public void writeTo(String templateReference,
			Viewable viewable, 
			MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            OutputStream out) throws IOException {
		final String resolvedPathStr = resolve(templateReference, mediaType);
		log.info(String.format("\n\t path: %s \n\t resolvedPath: %s \n\t viewable: %s \n\t req: %s",
				templateReference, 
				resolvedPathStr, 
				ToStringBuilder.reflectionToString(viewable), 
				req.getParameterMap()));
		try {
			out.write(process(resolvedPathStr, "/"));
		} catch (Exception e) {
			log.error("failed to process path:\n\t" + resolvedPathStr, e);
			throw new ContainerException(e);
		}
	}
	
	public byte[] process(final String currentPathStr, final String baseUriStr) throws Exception {
		if (StringUtils.isEmpty(currentPathStr)) {
			return new byte[0];
		}
		
		return IOUtils.toByteArray(getResAsInputStream(currentPathStr));
	}

	public InputStream getResAsInputStream(final String currentPathStr) throws MalformedURLException, IOException {
		final ServletContext ctx = req.getServletContext();
		final URL resUrl = ctx.getResource(currentPathStr); //TODO, need cache
		
		if (resUrl == null) {
			final String errorMsg = "request a non-exist path: " + currentPathStr;
			log.error(errorMsg);
			throw new FileNotFoundException(errorMsg);
		}
		
		log.info(resUrl.toString());
		final InputStream resIS = resUrl.openStream();
		return resIS;
	}
	
}
