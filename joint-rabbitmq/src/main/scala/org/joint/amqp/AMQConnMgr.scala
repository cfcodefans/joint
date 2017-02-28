package org.joint.amqp

import java.util.concurrent.{ConcurrentHashMap, Executors}
import java.util.{Comparator, Objects}

import com.rabbitmq.client._
import com.rabbitmq.client.impl.nio.NioParams
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.amqp.entity.{QueueCfg, ServerCfg}
import org.joint.amqp.logging.ConsumerLoggers._info
import org.joints.commons.MiscUtils

import scala.collection.convert.Wrappers.JConcurrentMapWrapper

/**
  * Created by fan on 2017/2/28.
  */
object AMQConnMgr {
    protected[amqp] val log: Logger = LogManager.getLogger(this.getClass)

    private val cfgAndConnCtxs: collection.concurrent.Map[ServerCfg, ConnContext] =
        JConcurrentMapWrapper[ServerCfg, ConnContext](new ConcurrentHashMap())

    def connCtx(sc: ServerCfg): ConnContext = synchronized({
        if (sc == null) null else cfgAndConnCtxs.getOrElseUpdate(sc, new ConnContext(_sc = sc))
    })

    def updateConnCtx(sc: ServerCfg): ConnContext = synchronized({
        val oldConnCtx: ConnContext = cfgAndConnCtxs(sc)
        if (oldConnCtx == null) return connCtx(sc)
        val newConnCtx: ConnContext = connCtx(sc)
        return newConnCtx.consumeForQueues(oldConnCtx.cfgAndChannels.keys)
    })

    def deleteConnCtx(sc: ServerCfg): ConnContext = synchronized({
        cfgAndConnCtxs.remove(sc).map(cc => {
            cc.close()
            cc
        }).get
    })

    private[amqp] def createConnFactory(sc: ServerCfg): ConnectionFactory = {
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

}

protected[amqp] class ConnContext(val sc: ServerCfg) extends ShutdownListener
    with RecoveryListener
    with BlockedListener
    with Comparable[NamedConnectionActor]
    with AutoCloseable {

    import AMQConnMgr._

    var connFactory: ConnectionFactory = _
    var conn: RecoverableConnection = _

    val cfgAndChannels: collection.concurrent.Map[QueueCfg, Channel] =
        JConcurrentMapWrapper[QueueCfg, Channel](new ConcurrentHashMap())

    def consumeForQueues(qcs: Iterable[QueueCfg]): Iterable[QueueCfg] = {
        return if (qcs.isEmpty) qcs else qcs.map(this.consumeForQueue(_))
    }

    def consumeForQueue(qc: QueueCfg): QueueCfg = {
        if (qc == null || !qc.isEnabled) return qc

    }

    def this(_sc: ServerCfg) {
        this(_sc)
        this.connFactory = createConnFactory(sc)
        this.conn = connFactory.newConnection(Executors.newSingleThreadExecutor(), s"conn-${sc.get()}").asInstanceOf
    }

    def canEqual(other: Any): Boolean = other.isInstanceOf[NamedConnectionActor]

    override def equals(other: Any): Boolean = other match {
        case that: NamedConnectionActor => (that canEqual this) && sc == that.sc
        case _ => false
    }

    override def hashCode(): Int = {
        return Objects.hash(sc)
    }

    override def compareTo(o: NamedConnectionActor): Int = Objects.compare(sc, o.sc, Comparator.naturalOrder())

    override def shutdownCompleted(cause: ShutdownSignalException): Unit = {
        val reason: Object = cause.getReason

        reason match {
            case (close: AMQP.Connection.Close) => {
                if (AMQP.CONNECTION_FORCED == close.getReplyCode && "OK" == close.getReplyText) {
                    val infoStr = s"\n force to close connection: to server: \n\t ${sc}"
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
        recoverable match {
            case conn: RecoverableConnection =>
                log.info(s"connection: ${conn.getId} to server \n\t $sc is recovered")
        }
    }

    override def handleRecoveryStarted(recoverable: Recoverable): Unit = {
        recoverable match {
            case conn: RecoverableConnection =>
                log.info(s"connection: ${conn.getId}to server \n\t $sc is being recovered")
        }
    }

    override def handleUnblocked(): Unit = {

    }

    override def handleBlocked(reason: String): Unit = {

    }

    override def close(): Unit = {
        conn.close()
    }
}

