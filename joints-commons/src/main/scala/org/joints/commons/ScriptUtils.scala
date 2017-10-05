package org.joints.commons

import java.util
import javax.script._

import jdk.nashorn.api.scripting.ScriptUtils
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.JavaConverters._

object ScriptUtils {
    private val log: Logger = LogManager.getLogger(classOf[ScriptUtils])

    private val sem: ScriptEngineManager = new ScriptEngineManager

    private val scriptEnginePool: ThreadLocal[util.Map[String, ScriptEngine]] =
        ThreadLocal.withInitial(() => new util.HashMap[String, ScriptEngine]())

    log.info("initialize ScriptEngineManager ")

    for (sef <- sem.getEngineFactories.asScala) {
        log.info(
            s"""
               |engine: {\n\t${sef.getEngineName},
               |version:\t ${sef.getExtensions},
               |mime_types:\t ${sef.getMimeTypes},
               |names:\t ${sef.getNames},
               |language_name:\t ${sef.getLanguageName},
               |language_version:\t ${sef.getLanguageVersion}
               |}
             """.stripMargin)
        sef.getMimeTypes.forEach((mimeType: String) => sem.registerEngineMimeType(mimeType, sef))
        sef.getExtensions.forEach((extension: String) => sem.registerEngineMimeType(extension, sef))
    }

    def getScriptEngineByMimeType(mimeType: String): ScriptEngine = {
        if (StringUtils.isBlank(mimeType)) return null
        val mineTypeAndScriptEngines: util.Map[String, ScriptEngine] = scriptEnginePool.get

        mineTypeAndScriptEngines.computeIfAbsent(mimeType, (_mimeType: String) => {
            val se: ScriptEngine = Option(sem.getEngineByMimeType(_mimeType)).getOrElse(sem.getEngineByExtension(_mimeType))
            if (se == null) log.error("not script engine found for mime _type: " + mimeType)
            se
        })
    }

    def getCompiledScript(mimeType: String, scriptStr: String): CompiledScript = {
        val errorMessage: String = s"ScriptEngine: ${mimeType} can't compile script: \n${scriptStr}"
        try {
            val se: ScriptEngine = getScriptEngineByMimeType(mimeType)
            if (se.isInstanceOf[Compilable]) {
                val compiler: Compilable = se.asInstanceOf[Compilable]
                return compiler.compile(scriptStr)
            }
            log.error(errorMessage)
        } catch {
            case e: ScriptException => log.error(errorMessage, e)
        }
        null
    }
}
