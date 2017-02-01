package org.joint.amqp.manager

import java.util.function.Consumer

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.amqp.entity.{MessageContext, QueueCfg}

import scala.collection.mutable

/**
  * Created by fan on 2017/1/31.
  */
object MsgMonitor {
    val _actor: ActorRef = ActorSystem(classOf[MsgMonitor].getSimpleName).actorOf(Props[MsgMonitor](new MsgMonitor))
    protected val log: Logger = LogManager.getLogger(classOf[MsgMonitor])

    def prefLog(mc: MessageContext, _log: Logger, infos: String*) {
        val dt = mc.getId
        if (dt % 100 == 11) _log.info("http performance: \t" + dt + " at \t" + System.currentTimeMillis)
    }
}

class MsgMonitor extends Actor with Consumer[MessageContext] {
    val qcAndMsgWatcher: mutable.Map[QueueCfg, ActorRef] = scala.collection.mutable.Map.empty[QueueCfg, ActorRef]

    override def accept(msgCtx: MessageContext): Unit = {
        qcAndMsgWatcher.get(msgCtx.queueCfg).map(ar => ar.tell(msgCtx, MsgMonitor._actor))
    }

    override def receive: Receive = {
        case (msgCtx: MessageContext) => accept(msgCtx)
    }
}