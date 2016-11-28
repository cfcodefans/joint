package org.joints.web.joint.script;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joints.commons.MiscUtils;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class ScriptExecutor {

	private static final Logger log = LogManager.getLogger(ScriptExecutor.class);
	
	protected ScriptExecutionContext scriptCtx;

	public ScriptExecutor(ScriptExecutionContext scriptCtx) {
		super();
		this.scriptCtx = scriptCtx;
	}

	public ScriptContext execute() {
		String mimeType = scriptCtx.mimeType;
		ScriptEngine se = ScriptUtils.getScriptEngineByMimeType(mimeType);
		
		String scriptStr = scriptCtx.getScriptStr();
		try {
			se.eval(scriptStr, scriptCtx.inflateScriptContext(se));
			scriptCtx.sc.getBindings(ScriptContext.ENGINE_SCOPE).putAll(se.getBindings(ScriptContext.ENGINE_SCOPE));
		} catch (ScriptException e) {
			log.info(MiscUtils.lineNumber(scriptStr));
			log.error(String.format("failed to execute script: \n\t %s \n\t", MiscUtils.lineNumber(scriptStr)), e);
		} finally {
			if (scriptCtx instanceof PageScriptExecutionContext) {
				PageScriptExecutionContext psc = (PageScriptExecutionContext) scriptCtx;
				psc.scriptElement.remove();
			}
		}
		return scriptCtx.sc;
	}
}
