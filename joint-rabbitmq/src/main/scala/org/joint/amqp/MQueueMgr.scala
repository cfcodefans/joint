package org.joint.amqp

import java.io.IOException
import java.util.Objects
import java.util.concurrent.{ExecutorService, Executors}

import com.rabbitmq.client._
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.amqp.entity.{MessageContext, QueueCfg, ServerCfg}
import org.joint.amqp.logging.ConsumerLoggers._
import org.joint.amqp.manager.MsgMonitor
import org.joint.failsafe.FailedMessageSqlStorage
import org.joint.http.HttpDispatcherActor
import org.joints.commons.MiscUtils

/**
  * Created by chenf on 2017/1/6.
  */
object MQueueMgr {
    def stopQueue(qc: QueueCfg) = ???

    protected val log: Logger = LogManager.getLogger(this.getClass)

    var NUM_CHANNEL_PER_CONN: Int = 2

    private val EXECUTORS: ExecutorService = Executors.newFixedThreadPool(MiscUtils.AVAILABLE_PROCESSORS * 2,
        MiscUtils.namedThreadFactory(this.getClass.getSimpleName))

    def connFactory(sc: ServerCfg): ConnectionFactory = {
        if (sc == null) return null
        new ConnectionFactory()
    }
}

object ConsumerActor {
    protected val log: Logger = LogManager.getLogger(classOf[ConsumerActor])
}

class ConsumerActor(val ch: Channel, val queueCfg: QueueCfg) extends DefaultConsumer(ch) {
    @throws[IOException]
    override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) {
        val d = new QueueingConsumer.Delivery(envelope, properties, body)
        val mc = new MessageContext(queueCfg, d)
        val sc = queueCfg.getServer
        _info(sc, "get message: " + d.getEnvelope.getDeliveryTag + " for q: " + queueCfg.getName + " on server " + sc.getVirtualHost)
        // TODO use injection to decouple dependency
        MsgMonitor._actor.!(mc)
        HttpDispatcherActor.instance.accept(mc)
    }
}

object QueueCtx {
    val log: Logger = LogManager.getLogger(classOf[QueueCtx])
}

class QueueCtx(val _ch: Channel, val _queueCfg: QueueCfg) extends ShutdownListener {

    import QueueCtx._

    val ch: Channel = _ch
    val qc: QueueCfg = _queueCfg

    override def equals(obj: Any): Boolean = {
        if (this eq obj.asInstanceOf[AnyRef]) return true
        if (obj == null) return false
        if (!obj.isInstanceOf[QueueCtx]) return false
        val other = obj.asInstanceOf[QueueCtx]
        return Objects.equals(qc, other.qc)
    }

    override def hashCode: Int = {
        return Objects.hashCode(qc)
    }

    override def shutdownCompleted(cause: ShutdownSignalException): Unit = {
        val reason: Object = cause.getReason
        val sc: ServerCfg = qc.getServer

        log.error("shutdown happens!!!", cause)
        if (reason.isInstanceOf[AMQP.Connection.Close]) {
            val close = reason.asInstanceOf[AMQP.Connection.Close]
            if (AMQP.CONNECTION_FORCED == close.getReplyCode && "OK" == close.getReplyText) {
                val infoStr = String.format("\n close connection to server: \n\t %s", sc)
                log.error(infoStr)
                _info(sc, infoStr)
                return
            }
        }
        if (reason.isInstanceOf[AMQP.Channel.Close]) {
            val close = reason.asInstanceOf[AMQP.Channel.Close]
            if (AMQP.CONNECTION_FORCED == close.getReplyCode && "OK" == close.getReplyText) {
                val infoStr = String.format("\n close channel to server: \n\t %s", sc)
                log.error(infoStr)
                _info(sc, infoStr)
                return
            }
        }
        if (cause.isHardError) {
            val infoStr = String.format("\n unexpected shutdown on connection to server: \n\t %s \n\n\t", sc, cause.getCause)
            log.error(infoStr)
            _info(sc, infoStr)
            return
        }
        MQueueMgr.stopQueue(qc)
    }
}

object Responder {
    protected val log: Logger = LogManager.getLogger(this.getClass)
    private val failsafe = FailedMessageSqlStorage.instance
}

