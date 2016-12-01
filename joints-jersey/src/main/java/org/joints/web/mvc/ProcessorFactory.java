package org.joints.web.mvc;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.ResolvedViewable;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;
import org.joints.commons.MiscUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.util.Map;

public class ProcessorFactory {
	public static final MediaType APPLICATION_XSLT = new MediaType("application", "xsl+xml");
	
	public static ResolvedViewable<String> getViewable(HttpServletResponse _resp,
                                                       HttpServletRequest _req,
                                                       String basePath,
                                                       String subPathStr,
                                                       Object model) {
		TemplateProcessor<String> _processor = null;
		final String extension = StringUtils.lowerCase(FilenameUtils.getExtension(subPathStr));
		MediaType mediaType = extensionAndMediaTypes.getOrDefault(extension, MediaType.WILDCARD_TYPE);
		
		if ("js".equals(extension)) {
			_processor = new JSProcessor(_req, _resp);
		} else if ("html".equals(extension)
				   || "htm".equals(extension)
				   || "xhtml".equals(extension)
				   || "xml".equals(extension) 
				   || "xsl".equals(extension)) {
			_processor = new DomViewProcessor(_req, _resp, basePath, mediaType);
		} else {
			_processor = new ResViewProcessor(_req, _resp, basePath, mediaType);
		}
		
		return new ResolvedViewable<String>(_processor,
				subPathStr, 
				new Viewable(subPathStr, model),
				mediaType);
	}
	
	@SuppressWarnings("unchecked")
	private static final Map<String, MediaType> extensionAndMediaTypes = MiscUtils.map(
			"js", MediaType.TEXT_PLAIN_TYPE,
			"html", MediaType.TEXT_HTML_TYPE,
			"htm", MediaType.TEXT_HTML_TYPE,
			"xhtml", MediaType.APPLICATION_XHTML_XML_TYPE,
			"xml", MediaType.TEXT_XML_TYPE,
			"xsl", MediaType.APPLICATION_XML_TYPE,
			"json", MediaType.APPLICATION_JSON_TYPE);
			
}
