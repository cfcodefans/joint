package org.joint.amqp.logging

import java.io.IOException

import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.RollingFileAppender
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy
import org.apache.logging.log4j.core.config.ConfigurationSource
import org.apache.logging.log4j.core.config.xml.XmlConfiguration
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.amqp.entity.ServerCfg


/**
  * Created by fan on 2017/1/31.
  */
object ConsumerLoggers {
    private[logging] val serverKeyAndLoggers: java.util.HashMap[String, Logger] = new java.util.HashMap[String, Logger]

    def getLoggerByServerCfg(sc: ServerCfg): Logger = serverKeyAndLoggers.computeIfAbsent(sc.getLogFilePath, (key: String) => createLogger(sc))

    private val APPENDER_NAME = "consumerDispatcherLog"

    private def createLogger(sc: ServerCfg) = {
        val logFileName: String = sc.getLogFilePath
        val maxLogSize: String = sc.getLogFileSize
        val logName: String = sc.getHost + "/" + sc.getVirtualHost
        val context: LoggerContext = LogManager.getContext.asInstanceOf[LoggerContext]
        val cfgSrc = context.getConfiguration.getConfigurationSource
        var xmlCfg: XmlConfiguration = null

        try {
            xmlCfg = new XmlConfiguration(context, new ConfigurationSource(FileUtils.openInputStream(cfgSrc.getFile), cfgSrc.getFile))
        } catch {
            case e: IOException => {
                e.printStackTrace()
            }
        }
        val lc = new LoggerContext(logName)
        lc.start(xmlCfg)
        val _appender: RollingFileAppender = xmlCfg.getAppender(APPENDER_NAME)
        val _rolloverStrategy = _appender.getManager().getRolloverStrategy

        val __appender: RollingFileAppender = RollingFileAppender.createAppender(logFileName,
            FilenameUtils.getBaseName(logFileName) + "-%d{yyyy-MM-dd}-%i.log",
            true.toString,
            logName,
            true.toString,
            8192.toString,
            false.toString,
            SizeBasedTriggeringPolicy.createPolicy(maxLogSize),
            _rolloverStrategy,
            _appender.getLayout,
            null,
            false.toString,
            false.toString,
            null,
            null)

        xmlCfg.getAppenders.keySet.forEach(xmlCfg.removeAppender)
        xmlCfg.addAppender(__appender)
        lc.getLogger(logName)
    }

    protected val log: Logger = LogManager.getLogger(this.getClass)

    def _info(sc: ServerCfg, infoStr: String) {
        val logForSrv = getLoggerByServerCfg(sc)
        if (logForSrv != null) logForSrv.info(infoStr)
    }

    def _info(sc: ServerCfg, format: String, args: Any*) {
        _info(sc, String.format(format, args))
    }

    def _info(_log: Logger, sc: ServerCfg, infoStr: String) {
        _log.info(infoStr)
        _info(sc, infoStr)
    }

    def _info(_log: Logger, sc: ServerCfg, format: String, args: Any*) {
        _info(_log, sc, String.format(format, args))
    }

    def _error(sc: ServerCfg, infoStr: String) {
        val logForSrv = getLoggerByServerCfg(sc)
        if (logForSrv != null) logForSrv.error(infoStr)
    }

    def _error(sc: ServerCfg, format: String, args: Any*) {
        val logForSrv = getLoggerByServerCfg(sc)
        if (logForSrv != null) logForSrv.error(String.format(format, args))
    }

    def _error(_log: Logger, sc: ServerCfg, infoStr: String) {
        _log.error(infoStr)
        _error(sc, infoStr)
    }

    def _error(sc: ServerCfg, infoStr: String, t: Throwable) {
        val logForSrv = getLoggerByServerCfg(sc)
        if (logForSrv != null) {
            logForSrv.error(infoStr, t)
        }
    }

    def _error(_log: Logger, sc: ServerCfg, infoStr: String, t: Throwable) {
        _log.error(infoStr, t)
        _error(sc, infoStr, t)
    }
}
