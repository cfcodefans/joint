package org.joints.web.mvc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joints.web.joint.script.PageScriptExecutionContext;
import org.joints.web.joint.script.ScriptExecutor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

import static org.joints.web.mvc.ResCacheMgr.CachedEntry;
import static org.joints.web.mvc.ViewProcModel.*;

public class DomViewProcessor extends ResViewProcessor {
	private static final Logger log = LogManager.getLogger(DomViewProcessor.class);

	public DomViewProcessor(final HttpServletRequest _req, final HttpServletResponse _resp, final String _basePathStr, MediaType mediaType) {
		super(_req, _resp, StringUtils.defaultIfBlank(_basePathStr, "/"), mediaType);
	}

	public DomViewProcessor(final HttpServletRequest _req, final HttpServletResponse _resp) {
		super(_req, _resp);
	}

	protected CachedEntry<ViewProcModel> createViewProcModel(final String currentPathStr, final String baseUriStr) {
		try {
			final InputStream resIS = getResAsInputStream(currentPathStr);
			final Document doc = Jsoup.parse(resIS, "UTF-8", baseUriStr, mediaType.getSubtype().contains("xml") ? Parser.xmlParser() : Parser.htmlParser());
			return new CachedEntry<ViewProcModel>(new ViewProcModel(doc, currentPathStr));
		} catch (Exception e) {
			log.error(String.format("failed to load resource from %s based on %s", currentPathStr, baseUriStr));
		}
		return null;
	}
	
	public byte[] process(final String currentPathStr, final String baseUriStr) throws Exception {
		if (StringUtils.isEmpty(currentPathStr)) {
			log.error("illegalArguement: currentPathStr is empty: " + currentPathStr);
			return new byte[0];
		}

		super.resp.setHeader(HttpHeaders.CONTENT_TYPE, mediaType.getType());
		CachedEntry<?> cachedEntry = ResCacheMgr.cache.asMap().computeIfAbsent(currentPathStr, (path)->createViewProcModel(path, baseUriStr));

		if (cachedEntry.content instanceof ViewProcModel) {
			ViewProcModel vpm = (ViewProcModel) cachedEntry.content;
			ViewFacade vf = vpm.getViewFacade();
			Element doc = vf._doc;
			ScriptContext sc = new SimpleScriptContext();

			for (final Pair<Element, String> elementAndScript : vf.scriptElementList) {
				final ScriptExecutor se = new ScriptExecutor(new PageScriptExecutionContext(doc, elementAndScript.getKey(), req, resp, currentPathStr, sc));
				sc = se.execute();
			}

			return doc.html().getBytes();
		} else {
			log.error(String.format("the path: %s is not cached correctly", super.resolve(currentPathStr, mediaType)));
		}

		return String.format("can't render this as html: \n%s", String.valueOf(cachedEntry)).getBytes();
	}
}
