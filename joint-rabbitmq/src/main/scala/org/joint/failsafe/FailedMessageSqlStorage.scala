package org.joint.failsafe

import java.util
import java.util.concurrent.{ExecutorService, Executors}
import java.util.function.Consumer
import javax.persistence.EntityManager

import akka.actor.{ActorRef, ActorSystem, Props}
import org.apache.commons.collections4.CollectionUtils
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.akka.BatchActor
import org.joint.amqp.entity.MessageContext
import org.joint.http.HttpDispatcherActor
import org.joints.commons.MiscUtils
import org.joints.commons.persistence.jpa.cdi.TransactionalInterceptor

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by fan on 2017/2/2.
  */
object FailedMessageSqlStorage {
    protected val log: Logger = LogManager.getLogger(classOf[FailedMessageSqlStorage])

    private val executor: ExecutorService = Executors.newSingleThreadExecutor(
        MiscUtils.namedThreadFactory(classOf[FailedMessageSqlStorage].getSimpleName))

    private val system: ActorSystem = ActorSystem(name = classOf[FailedMessageSqlStorage].getSimpleName,
        defaultExecutionContext = Option(ExecutionContext.fromExecutorService(executor)))

    lazy val instance: ActorRef = system.actorOf(Props[FailedMessageSqlStorage](new FailedMessageSqlStorage))

    private val batchSize: Int = 100
    private val batchInterval: Long = 1
}

import org.joint.failsafe.FailedMessageSqlStorage._

class FailedMessageSqlStorage extends BatchActor[MessageContext](batchInterval.millisecond, batchSize) with Consumer[util.Collection[MessageContext]] {
    private val lastBatchTime: Long = 0

    override def accept(mcList: util.Collection[MessageContext]): Unit = {
        if (CollectionUtils.isEmpty(mcList)) return
        TransactionalInterceptor.withTransaction(saveFailedMessageContexts, mcList)
    }

    def saveFailedMessageContexts(em: EntityManager, mcList: util.Collection[MessageContext]): Unit = {
        for (mc: MessageContext <- mcList.asScala.toList.distinct) {
            saveFailedMessageContext(em, mc)
        }
    }

    def saveFailedMessageContext(em: EntityManager, mc: MessageContext): Unit = {
        if (mc.isSucceeded && mc.id > 0) {
            val q = em.createQuery("delete from MessageContext mc where mc.id=:id")
            q.setParameter("id", mc.id)
            q.executeUpdate
            return
        }

        var _mc: MessageContext = if (mc.id > 0) em.find(classOf[MessageContext], mc.getId) else null
        if (_mc == null) {
            _mc = em.merge(mc)
            mc.setId(_mc.id)
            HttpDispatcherActor.instance.accept(mc)
            return
        }

        _mc.setDelivery(MessageContext.clone(mc.getDelivery))
        _mc.setFailTimes(mc.getFailTimes)
        _mc.setResponse(mc.getResponse)
        em.merge(_mc)
    }

    override def processBatch(batch: util.Collection[MessageContext]): Unit = accept(batch)
}
