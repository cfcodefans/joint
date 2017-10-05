package org.joints.web.joint.script;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joints.web.mvc.ResCacheMgr;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class PageScriptExecutionContext extends ScriptExecutionContext {
	public Element document;
	public Element scriptElement;
	
//	private static Log log = LogFactory.getLog(PageScriptExecutionContext.class);

	public static String getScriptStr(ServletContext servletContext, String refPath, Element scriptElement) {
		String srcPath = scriptElement.attr("src");
		if (StringUtils.isBlank(srcPath)) {
			return getScriptStr(scriptElement);
		}
		return ResCacheMgr.getTextResource(refPath, srcPath);
	}

	public static String getScriptStr(Element scriptElement) {
		if (scriptElement == null) {
			return StringUtils.EMPTY;
		}
		List<TextNode> textNodes = scriptElement.textNodes();
		return (CollectionUtils.isNotEmpty(textNodes)) ? textNodes.get(0).getWholeText() : scriptElement.data();
	}

	public PageScriptExecutionContext(final Element document, 
								 final Element scriptElement, 
								 final HttpServletRequest req, 
								 final HttpServletResponse resp, 
								 final String basePathStr,
								 final ScriptContext sc) {
		super(null, req, resp, basePathStr, sc, scriptElement.attr("_type"));
		super.scriptStr = getScriptStr(req.getServletContext(), basePathStr, scriptElement);
		this.document = document;
		this.scriptElement = scriptElement;
	}

	public ScriptContext inflateScriptContext(final ScriptEngine se) {
		if (sc == null) {
			sc = new SimpleScriptContext();
		}
		
		Bindings bindings = sc.getBindings(ScriptContext.GLOBAL_SCOPE);
		if (bindings == null) {
			bindings = se.createBindings();
			sc.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
		}
		
		bindings.put("doc", document);
		bindings.put("req", req);
		bindings.put("resp", resp);
		bindings.put("me", scriptElement);

		return sc;
	}
	
}