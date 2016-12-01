package org.joints.web.joint.script;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joints.commons.MiscUtils;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * @author fan
 * The compiled scripts could not share context between each other, not really practical.
 */
@Deprecated
public class CompiledScriptExecutor extends ScriptExecutor {
	private static final Logger log = LogManager.getLogger(CompiledScriptExecutor.class);
	
	protected final CompiledScript compiledScript;
	
	public CompiledScriptExecutor(ScriptExecutionContext scriptCtx, CompiledScript compiledScript) {
		super(scriptCtx);
		this.compiledScript  = compiledScript;
	}

	
	public ScriptContext execute() {
		if (compiledScript == null) {
			return super.execute();
		}
		
		try {
			ScriptEngine se = compiledScript.getEngine();
			compiledScript.eval(scriptCtx.inflateScriptContext(se));
			scriptCtx.sc.getBindings(ScriptContext.ENGINE_SCOPE).putAll(se.getBindings(ScriptContext.ENGINE_SCOPE));
		} catch (ScriptException e) {
			String scriptStr = scriptCtx.scriptStr;
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
