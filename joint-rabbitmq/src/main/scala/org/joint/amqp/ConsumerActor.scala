package org.joint.amqp

import java.io.IOException

import com.rabbitmq.client._
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.amqp.entity.{MessageContext, QueueCfg}
import org.joint.amqp.manager.MsgMonitor
import org.joint.http.HttpDispatcherActor

/**
  * Created by fan on 2017/2/24.
  */
object ConsumerActor {
    protected val log: Logger = LogManager.getLogger(classOf[ConsumerActor])
}

class ConsumerActor(val ch: Channel, val queueCfg: QueueCfg) extends DefaultConsumer(ch) {

    import org.joint.amqp.logging.ConsumerLoggers._

    @throws[IOException]
    override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) {
        val d = new QueueingConsumer.Delivery(envelope, properties, body)
        val mc = new MessageContext(queueCfg, d)
        val sc = queueCfg.getServer
        _info(sc, s"get message: ${d.getEnvelope.getDeliveryTag} for q: ${queueCfg.getName} on server ${sc.getVirtualHost}")
        // TODO use injection to decouple dependency
        MsgMonitor._actor.!(mc)
        HttpDispatcherActor.instance.accept(mc)
    }
}