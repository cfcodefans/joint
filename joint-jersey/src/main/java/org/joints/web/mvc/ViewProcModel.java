package org.joints.web.mvc;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.joints.web.joint.script.PageScriptExecutionContext;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ViewProcModel implements Serializable {

	/**
	 * Could not find a way to make compiled scripts share the context so far, it isn't practical
	 * 
	 * @param mimeType
	 * @param srcPath
	 * @return
	 */
	// @Deprecated
	// There is some critical issue with compiled script, as it doesn't share script context
	// within scope of a page, several script elements would be compiled separately
	// and they can not share variables in a context, this greatly limits the application of scripts
	// private CompiledScript loadCompileScript(String mimeType, String srcPath) {
	// if (StringUtils.isBlank(mimeType) || StringUtils.isBlank(srcPath)) {
	// return null;
	// }
	// final String _mimeType = mimeType.trim();
	// final String _srcPath = srcPath.trim();
	//
	// String absoluteResPath = ResCacheMgr.getAbsoluteResPath(viewPath, _srcPath);
	// Object content = ResCacheMgr.cache.get(absoluteResPath, () -> {
	// return new ResCacheMgr.CachedEntry<CompiledScript>(ScriptUtils.getCompiledScript(_mimeType,
	// ResCacheMgr.getTextResource(viewPath, _srcPath)));
	// }).content;
	// return (CompiledScript) content;
	// }

	private class ScriptCtxModel implements Serializable {
		private static final long	serialVersionUID	= 1L;
		private final Integer[]		path;

		// private CompiledScript compiledScript = null;

		public ScriptCtxModel(Element root, Element scriptElement) {
			super();
			this.path = elementPath(root, scriptElement);
		}

		public Element getScriptElementByPath(final Element cloneOfRoot) {
			return getElementByPath(cloneOfRoot, path);
		}
		
		// @Deprecated
		// public CompiledScript getCompiledScript(Element scriptElement) {
		// String srcPath = scriptElement.attr("src");
		// String type = scriptElement.attr("type");
		//
		// if (StringUtils.isBlank(srcPath)) {
		// if (compiledScript != null) {
		// return compiledScript;
		// }
		//
		// compiledScript = ScriptUtils.getCompiledScript(type, ScriptUtils.getScriptStr(scriptElement));
		// return compiledScript;
		// }
		//
		// return loadCompileScript(type, srcPath);
		// }
	}

	public static class ViewFacade {
		public final Element								_doc;
//		@Deprecated
		public final List<Pair<Element, String>> scriptElementList;

		public ViewFacade(Element _doc, @Deprecated List<Pair<Element, String>> scriptElementList) {
			super();
			this._doc = _doc;
			this.scriptElementList = scriptElementList;
		}
	}

	public ViewFacade getViewFacade() {
		// make a clone of the original html/xml document
		final Element _doc = doc.clone();
		// use the path to locate the script elements for better performance
		List<Pair<Element, String>> _scriptElementList = scriptCtxModelList.stream().map(cm -> {
				Element scriptElement = cm.getScriptElementByPath(_doc);
				return new ImmutablePair<Element, String>(scriptElement, PageScriptExecutionContext.getScriptStr(scriptElement));
			}).collect(Collectors.toList());
		return new ViewFacade(_doc, _scriptElementList);
	}

	private static final long			serialVersionUID	= 1L;
	public final Element				doc;
	public final List<ScriptCtxModel> scriptCtxModelList	= new ArrayList<ScriptCtxModel>();
	public final String viewPath;

	public ViewProcModel(Element domRoot, String viewPath) {
		super();
		this.doc = domRoot;
		this.viewPath = viewPath;
		final Elements els = doc.select("script[data-runat=server]");
		els.stream().map(el -> new ScriptCtxModel(doc, el)).forEach(scriptCtxModelList::add);
	}

	public List<ScriptCtxModel> getScriptCtxModelList() {
		return scriptCtxModelList;
	}

	public static Integer[] elementPath(Element root, Element _element) {
		if (root == null || _element == null) {
			return new Integer[0];
		}
		LinkedList<Integer> idxList = new LinkedList<Integer>();
		for (Element currentElement = _element; currentElement != root; currentElement = currentElement.parent()) {
			if (currentElement == null) {
				// the root is not ancestor of _element
				return new Integer[0];
			}
			idxList.addFirst(currentElement.elementSiblingIndex());
		}

		return idxList.toArray(new Integer[0]);
	}

	public static Element getElementByPath(final Element start, Integer[] path) {
		if (start == null || ArrayUtils.isEmpty(path)) {
			return null;
		}

		Element _element = start;
		for (Integer idx : path) {
			_element = _element.child(idx);
			if (_element == null) {
				// wrong path
				return null;
			}
		}

		return _element;
	}
}
