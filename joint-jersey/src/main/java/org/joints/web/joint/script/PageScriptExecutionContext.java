package org.joints.web.joint.script;

import org.jsoup.nodes.Element;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PageScriptExecutionContext extends ScriptExecutionContext {
	public Element document;
	public Element scriptElement;
	
//	private static Log log = LogFactory.getLog(PageScriptExecutionContext.class);
	
	public PageScriptExecutionContext(final Element document, 
								 final Element scriptElement, 
								 final HttpServletRequest req, 
								 final HttpServletResponse resp, 
								 final String basePathStr,
								 final ScriptContext sc) {
		super(null, req, resp, basePathStr, sc, scriptElement.attr("type"));
		super.scriptStr = ScriptUtils.getScriptStr(req.getServletContext(), basePathStr, scriptElement);
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