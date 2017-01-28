package org.joints.commons;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.*;
import java.util.HashMap;
import java.util.Map;

public class ScriptUtils {
    private static final Logger log = LogManager.getLogger(ScriptUtils.class);

    public static CompiledScript getCompiledScript(String mimeType, String scriptStr) {
        String errorMessage = String.format("ScriptEngine: %s can't compile script: \n%s", mimeType, scriptStr);
        try {
            ScriptEngine se = getScriptEngineByMimeType(mimeType);
            if (se instanceof Compilable) {
                Compilable compiler = (Compilable) se;
                return compiler.compile(scriptStr);
            }
            log.error(errorMessage);
        } catch (ScriptException e) {
            log.error(errorMessage, e);
        }
        return null;
    }

    public static ScriptEngine getScriptEngineByMimeType(String mimeType) {
        if (StringUtils.isBlank(mimeType)) return null;

        Map<String, ScriptEngine> mineTypeAndScriptEngines = scriptEnginePool.get();
        return mineTypeAndScriptEngines.computeIfAbsent(mimeType, (_mimeType) -> {
            ScriptEngine se = sem.getEngineByMimeType(_mimeType);
            if (se == null) {
                se = sem.getEngineByExtension(_mimeType);
            }
            if (se == null) {
                log.error("not script engine found for mime type: " + mimeType);
            }
            return se;
        });
    }

    private static final ThreadLocal<Map<String, ScriptEngine>> scriptEnginePool = ThreadLocal.withInitial(HashMap<String, ScriptEngine>::new);

    private static ScriptEngineManager sem = new ScriptEngineManager();

    static {
        log.info("initialize ScriptEngineManager ");
        for (ScriptEngineFactory sef : sem.getEngineFactories()) {
            final String _str = "engine: { \n\tname: " + sef.getEngineName()
                + ", \n\tversion: " + sef.getEngineVersion()
                + ", \n\textensions: " + sef.getExtensions()
                + ", \n\tmime_types: " + sef.getMimeTypes()
                + ", \n\tnames: " + sef.getNames()
                + ", \n\tlanguage_name: " + sef.getLanguageName()
                + ", \n\tlanguage_version: " + sef.getLanguageVersion() + "\n}\n";

            log.info(_str);

            sef.getMimeTypes().forEach(mimeType -> sem.registerEngineMimeType(mimeType, sef));
            sef.getExtensions().forEach(extension -> sem.registerEngineMimeType(extension, sef));
        }
    }

}
