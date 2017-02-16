package org.joint.failsafe

import java.util
import java.util.function.Consumer

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.amqp.entity.MessageContext

/**
  * Created by fan on 2017/2/2.
  */
object FailedMessageSqlStorage {
    protected val log: Logger = LogManager.getLogger(classOf[FailedMessageSqlStorage])

    private val system: ActorSystem = ActorSystem(classOf[FailedMessageSqlStorage].getSimpleName)
    lazy val instance: ActorRef = system.actorOf(Props[FailedMessageSqlStorage](new FailedMessageSqlStorage))
    private val batchSize: Int = 100
    private val batchInterval: Long = 1000
}

class FailedMessageSqlStorage extends Actor with Consumer[MessageContext] {

    import FailedMessageSqlStorage._

    private var em = null

    private val batchList: util.LinkedList[MessageContext] = new util.LinkedList[MessageContext]()
    private val lastBatchTime: Long = 0

    override def receive: Receive = {
        case (msgCtx: MessageContext) => accept(msgCtx)
    }

    override def accept(mc: MessageContext): Unit = {
        if (batchList.size() <= batchSize) {
            batchList.add(mc)
            return
        }
    }
}
