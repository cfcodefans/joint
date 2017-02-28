package org.joint.amqp

import java.util.concurrent.{ConcurrentHashMap, ExecutorService, Executors}
import java.util.{Comparator, Objects}

import akka.actor.{Actor, ActorRef}
import com.rabbitmq.client._
import com.rabbitmq.client.impl.nio.NioParams
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.amqp.entity.{MessageContext, QueueCfg, ServerCfg}
import org.joint.amqp.logging.ConsumerLoggers._
import org.joint.failsafe.FailedMessageSqlStorage
import org.joints.commons.MiscUtils

import scala.collection.convert.Wrappers.JConcurrentMapWrapper
import scala.collection.mutable

/**
  * Created by chenf on 2017/1/6.
  */
object MQueueMgr {
    //    def stopQueue(qc: QueueCfg) = ???

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

    def connFactory(qc: QueueCfg): ConnectionFactory = if (qc != null) connFactory(qc.getServer) else null

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

    def updateConnFactory(sc: ServerCfg): ConnectionFactory = {
        if (sc == null) return null
        return cfgAndConnFactories.replace(sc, createConnFactory(sc)).get
    }

    def removeConnFactory(sc: ServerCfg): ConnectionFactory = cfgAndConnFactories.remove(sc).get
}

object ConnectionMgr {
    protected[amqp] val log: Logger = LogManager.getLogger(this.getClass)

    private val EXECUTORS: ExecutorService = Executors.newFixedThreadPool(
        MiscUtils.AVAILABLE_PROCESSORS * 2,
        MiscUtils.namedThreadFactory(this.getClass.getSimpleName))

    private[amqp] val serverCfgAndNamedConnections: collection.concurrent.Map[QueueCfg, NamedConnectionActor] =
        JConcurrentMapWrapper[QueueCfg, NamedConnectionActor](new ConcurrentHashMap())

    def createConn(qc: QueueCfg): NamedConnectionActor = {
        if (qc == null) return null
        val cf: ConnectionFactory = ConnFactoryMgr.connFactory(qc)
        if (cf == null) {
            log.error(s"failed to get ConnectionFactory for ServerCfg:\n\t$qc")
            return null
        }

        val connName: String = s"conn-${qc.getServer.get().toASCIIString}"
        val conn: RecoverableConnection = cf.newConnection(EXECUTORS, connName).asInstanceOf[RecoverableConnection]
        val named: NamedConnectionActor = new NamedConnectionActor(connName: String, conn, qc.getServer)

        conn.addShutdownListener(named)
        conn.addRecoveryListener(named)
        conn.addBlockedListener(named)

        return named
    }

    def getConn(qc: QueueCfg): NamedConnectionActor = if (qc == null) null else serverCfgAndNamedConnections.getOrElseUpdate(qc, createConn(qc))
}

class NamedConnectionActor(val name: String,
                           val conn: RecoverableConnection,
                           val sc: ServerCfg,
                           val queueCfgs: mutable.Set[QueueCfg] = mutable.Set.empty)
    extends Actor
        with ShutdownListener
        with RecoveryListener
        with BlockedListener
        with Comparable[NamedConnectionActor] {

    import ConnectionMgr._

    def canEqual(other: Any): Boolean = other.isInstanceOf[NamedConnectionActor]

    override def equals(other: Any): Boolean = other match {
        case that: NamedConnectionActor => (that canEqual this) && name == that.name && sc == that.sc
        case _ => false
    }

    override def hashCode(): Int = {
        return Objects.hash(name, sc)
    }

    override def compareTo(o: NamedConnectionActor): Int = Objects.compare(name, o.name, Comparator.naturalOrder())

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
            case (unknown: _) => {
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

    override def receive: Receive = {

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
        //        MQueueMgr.stopQueue(qc)
    }
}

object Responder {
    protected val log: Logger = LogManager.getLogger(this.getClass)
    private val failsafe: ActorRef = FailedMessageSqlStorage.instance
    private val executor: ExecutorService = Executors.newFixedThreadPool(MiscUtils.AVAILABLE_PROCESSORS, MiscUtils.namedThreadFactory("Responder"))

    override def finalize(): Unit = {
        executor.shutdownNow()
        super.finalize()
    }
}

class Responder extends Actor {
    override def receive: Receive = {
        case (mc: MessageContext) => {
            if (mc.isSucceeded) {

            }
        }
    }
}

