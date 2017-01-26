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

    private class ScriptCtxModel implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Integer[] path;

        // private CompiledScript compiledScript = null;

        public ScriptCtxModel(Element root, Element scriptElement) {
            super();
            this.path = elementPath(root, scriptElement);
        }

        public Element getScriptElementByPath(final Element cloneOfRoot) {
            return getElementByPath(cloneOfRoot, path);
        }

    }

    public static class ViewFacade {
        public final Element _doc;
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

    private static final long serialVersionUID = 1L;
    public final Element doc;
    public final List<ScriptCtxModel> scriptCtxModelList = new ArrayList<ScriptCtxModel>();
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
                return null;
            }
        }

        return _element;
    }
}
