package org.joint.amqp

import java.util.concurrent.{ConcurrentHashMap, ExecutorService, Executors}
import java.util.{Comparator, Objects}

import akka.actor.{Actor, ActorRef}
import com.rabbitmq.client._
import com.rabbitmq.client.impl.nio.NioParams
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.amqp.entity.{MessageContext, QueueCfg, ServerCfg}
import org.joint.amqp.logging.ConsumerLoggers._
import org.joint.failsafe.FailedMessageSqlStorage
import org.joints.commons.MiscUtils

import scala.collection.JavaConverters._
import scala.collection.convert.Wrappers.JConcurrentMapWrapper

/**
  * Created by chenf on 2017/1/6.
  */
object MQueueMgr {
    //    def stopQueue(sc: QueueCfg) = ???

    protected val log: Logger = LogManager.getLogger(this.getClass)

    var NUM_CHANNEL_PER_CONN: Int = 2


}

object ConnFactoryMgr {
    protected val log: Logger = LogManager.getLogger(this.getClass)

    private val cfgAndConnFactories: collection.concurrent.Map[ServerCfg, ConnectionFactory] =
        JConcurrentMapWrapper[ServerCfg, ConnectionFactory](new ConcurrentHashMap())

    def connFactory(sc: ServerCfg): ConnectionFactory = {
        if (sc == null) return null
        return cfgAndConnFactories.getOrElseUpdate(sc, createConnFactory(sc))
    }

    def updateConnFactory(sc: ServerCfg): ConnectionFactory = synchronized({
        if (sc == null) return null
        ConnectionMgr.closeConnBy(sc)
        val newFactory: ConnectionFactory = cfgAndConnFactories.replace(sc, createConnFactory(sc)).get
        ConnectionMgr.openConnFor(sc)
        return newFactory
    })

    private def createConnFactory(sc: ServerCfg): ConnectionFactory = {
        val connFactory: ConnectionFactory = new ConnectionFactory()
        connFactory.setHost(sc.getHost)
        connFactory.setPort(sc.getPort)
        connFactory.setUsername(sc.getUsername)
        connFactory.setPassword(sc.getPassword)
        connFactory.setVirtualHost(sc.getVirtualHost)
        connFactory.setAutomaticRecoveryEnabled(true)
        connFactory.useNio()

        val nioParams: NioParams = new NioParams()
        nioParams.setNbIoThreads(Math.min(MiscUtils.AVAILABLE_PROCESSORS / 2, 2))
        connFactory.setNioParams(nioParams)

        return connFactory
    }

    def removeConnFactory(sc: ServerCfg): ConnectionFactory = {
        ConnectionMgr.closeConnBy(sc)
        return cfgAndConnFactories.remove(sc).get
    }
}

object ConnectionMgr {
    protected[amqp] val log: Logger = LogManager.getLogger(this.getClass)
    private[amqp] val serverCfgAndNamedConnections: collection.concurrent.Map[ServerCfg, ConnectionWrapper] =
        JConcurrentMapWrapper[ServerCfg, ConnectionWrapper](new ConcurrentHashMap())

    def closeConnBy(sc: ServerCfg): ConnectionWrapper = synchronized({
        return serverCfgAndNamedConnections.remove(sc).map(cw => {
            cw.conn.close(AMQP.CONNECTION_FORCED, "OK")
            cw
        }).get
    })

    def openConnFor(sc: ServerCfg): ConnectionWrapper = synchronized({
        val cf: ConnectionFactory = ConnFactoryMgr.connFactory(sc)
        val connWrapper: ConnectionWrapper = getOrCreateConnection(sc)
        connWrapper.conn = cf.newConnection(getExecutorsForConn, connWrapper.name).asInstanceOf[RecoverableConnection]
        connWrapper.conn.addShutdownListener(connWrapper)
        connWrapper.conn.addRecoveryListener(connWrapper)
        connWrapper.conn.addBlockedListener(connWrapper)
        return connWrapper
    })

    def getOrCreateConnection(sc: ServerCfg): ConnectionWrapper = {
        return if (sc == null) null else serverCfgAndNamedConnections.getOrElseUpdate(sc, createConn(sc))
    }

    def createConn(sc: ServerCfg): ConnectionWrapper = {
        if (sc == null) return null
        val cf: ConnectionFactory = ConnFactoryMgr.connFactory(sc)
        if (cf == null) {
            log.error(s"failed to get ConnectionFactory for ServerCfg:\n\t$sc")
            return null
        }

        val connName: String = s"conn-${sc.get().toASCIIString}"
        val conn: RecoverableConnection = cf.newConnection(getExecutorsForConn, connName).asInstanceOf[RecoverableConnection]
        val named: ConnectionWrapper = new ConnectionWrapper(connName, conn, sc)

        conn.addShutdownListener(named)
        conn.addRecoveryListener(named)
        conn.addBlockedListener(named)

        return named
    }

    private def getExecutorsForConn: ExecutorService = {
        return Executors.newFixedThreadPool(
            MiscUtils.AVAILABLE_PROCESSORS * 2,
            MiscUtils.namedThreadFactory(this.getClass.getSimpleName))
    }
}

class ConnectionWrapper(val name: String,
                        var conn: RecoverableConnection,
                        var sc: ServerCfg) extends Actor
    with ShutdownListener
    with RecoveryListener
    with BlockedListener
    with Comparable[ConnectionWrapper] {

    import ConnectionMgr._

    override def equals(other: Any): Boolean = other match {
        case that: ConnectionWrapper => (that canEqual this) && name == that.name && sc == that.sc
        case _ => false
    }

    def canEqual(other: Any): Boolean = other.isInstanceOf[ConnectionWrapper]

    override def hashCode(): Int = Objects.hash(name, sc)

    override def compareTo(o: ConnectionWrapper): Int = Objects.compare[String](name, o.name, Comparator.naturalOrder())

    override def shutdownCompleted(cause: ShutdownSignalException): Unit = {
        val reason: Object = cause.getReason

        reason match {
            case (close: AMQP.Connection.Close) => {
                if (AMQP.CONNECTION_FORCED == close.getReplyCode && "OK" == close.getReplyText) {
                    val infoStr = s"\n force to close connection to server: \n\t ${sc}"
                    log.error(infoStr)
                    _info(sc, infoStr)
                }
            }
            case unknown@_ => {
                log.error(s"$unknown closed connection to server: \n\t $sc")
            }
        }
    }

    override def handleRecovery(recoverable: Recoverable): Unit = {
        log.info(s"connection: $name to server \n\t $sc is recovered")
    }

    override def handleRecoveryStarted(recoverable: Recoverable): Unit = {
        log.info(s"connection: $name to server \n\t $sc is being recovered")
    }

    override def handleUnblocked(): Unit = {

    }

    override def handleBlocked(reason: String): Unit = {

    }

    override def receive: Receive = ???
}


object QueueCtx {
    val log: Logger = LogManager.getLogger(classOf[QueueCtx])

    private[amqp] val queueCfgAndCtxs: collection.concurrent.Map[QueueCfg, QueueCtx] =
        JConcurrentMapWrapper[QueueCfg, QueueCtx](new ConcurrentHashMap())

    def getOrCreateQueueCtx(qc: QueueCfg): QueueCtx = synchronized({
        if (qc == null || qc.server == null) return null
        return queueCfgAndCtxs.getOrElseUpdate(qc, createQueueCtx(qc))
    })

    private[amqp] def createQueueCtx(qc: QueueCfg): QueueCtx = {
        val connWrapper: ConnectionWrapper = ConnectionMgr.getOrCreateConnection(qc.getServer)
        if (connWrapper == null) return null

        val ch: Channel = connWrapper.conn.createChannel()
        val queueCtx: QueueCtx = new QueueCtx(ch, qc)
        ch.addShutdownListener(queueCtx)

        val queueName = qc.getName
        ch.queueDeclare(queueName, qc.isDurable, qc.isExclusive, qc.isAutoDelete, null)
        if (qc.getPreferFetchSize != null) {
            ch.basicQos(qc.getPreferFetchSize)
        }
        val routeKey = qc.getRouteKey
        for (ec <- qc.getExchanges.asScala) {
            val exchangeName = StringUtils.defaultString(ec.getName, StringUtils.EMPTY)
            ch.exchangeDeclare(exchangeName, ec.getExchangeType, ec.isDurable, ec.isAutoDelete, null)
            ch.queueBind(queueName, exchangeName, routeKey)
        }

        ch.setDefaultConsumer(new ConsumerActor(ch, qc))

        return queueCtx
    }

    def closeQueueCtx(qc: QueueCfg): QueueCtx = synchronized({
        return queueCfgAndCtxs.remove(qc).map(ctx => {
            ctx._ch.close(AMQP.CONNECTION_FORCED, "OK")
            ctx
        }).get
    })
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
                val infoStr = s"\n close connection to server: \n\t ${sc}"
                log.error(infoStr)
                _info(sc, infoStr)
                return
            }
        }
        if (reason.isInstanceOf[AMQP.Channel.Close]) {
            val close = reason.asInstanceOf[AMQP.Channel.Close]
            if (AMQP.CONNECTION_FORCED == close.getReplyCode && "OK" == close.getReplyText) {
                val infoStr = s"\n close channel to server: \n\t ${sc}"
                log.error(infoStr)
                _info(sc, infoStr)
                return
            }
        }
        if (cause.isHardError) {
            val infoStr = s"\n unexpected shutdown on connection to server: \n\t ${sc} \n\n\t${cause.getMessage}"
            log.error(infoStr)
            _info(sc, infoStr)
            return
        }
    }
}

object QueueResponder {
    protected val log: Logger = LogManager.getLogger(this.getClass)
    private val failsafe: ActorRef = FailedMessageSqlStorage.instance
    private val executor: ExecutorService = Executors.newFixedThreadPool(MiscUtils.AVAILABLE_PROCESSORS,
        MiscUtils.namedThreadFactory("QueueResponder"))

    override def finalize(): Unit = {
        executor.shutdownNow()
        super.finalize()
    }
}

class QueueResponder extends Actor {
    override def receive: Receive = {
        case (mc: MessageContext) => {
            val queueCtx: QueueCtx = QueueCtx.getOrCreateQueueCtx(mc.queueCfg)

            if (mc.isSucceeded) {
                queueCtx._ch.basicAck(mc.getDelivery().getEnvelope.getDeliveryTag, false)
                if (mc.getFailTimes > 0) {
                    FailedMessageSqlStorage.instance.tell(mc, this.self)
                }
            } else {
                FailedMessageSqlStorage.instance.tell(mc, this.self)
                if (mc.isExceedFailTimes) {
                    queueCtx._ch.basicAck(mc.getDelivery().getEnvelope.getDeliveryTag, false)
                }
            }
        }
    }
}

