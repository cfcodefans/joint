package org.joint.amqp.entity

import java.io.Serializable
import java.util.Arrays
import javax.persistence._
import javax.xml.bind.annotation.{XmlRootElement, XmlTransient}

import com.fasterxml.jackson.annotation.JsonIgnore
import com.rabbitmq.client.QueueingConsumer.Delivery
import com.rabbitmq.client.{AMQP, Envelope, QueueingConsumer}
import org.apache.commons.lang3.{ObjectUtils, StringUtils}

import scala.beans.BeanProperty

object MessageContext {
    val DEFAULT_RETRY_LIMIT: Int = 100

    def clone(d: QueueingConsumer.Delivery): QueueingConsumer.Delivery = {
        if (d == null) return d
        new QueueingConsumer.Delivery(clone(d.getEnvelope), clone(d.getProperties), ObjectUtils.clone(d.getBody))
    }

    def clone(ev: Envelope): Envelope = {
        if (ev == null) return ev
        new Envelope(ev.getDeliveryTag, ev.isRedeliver, ev.getExchange, ev.getRoutingKey)
    }

    def clone(p: AMQP.BasicProperties): AMQP.BasicProperties = {
        if (p == null) return p
        new AMQP.BasicProperties(p.getContentType, p.getContentEncoding, p.getHeaders, p.getDeliveryMode, p.getPriority, p.getCorrelationId, p.getReplyTo, p.getExpiration, p.getMessageId, p.getTimestamp, p.getType, p.getUserId, p.getAppId, p.getClusterId)
    }

}

/**
  * Created by fan on 2017/1/30.
  */
@XmlRootElement
@Entity
@Table(name = "msg_ctx", indexes = Array(new Index(columnList = "")))
@Cacheable(false)
class MessageContext extends Serializable with Cloneable {

    import MessageContext._

    def this(qc: QueueCfg, d: Delivery) = {
        this()
        this.queueCfg = qc
        this.setDelivery(d)
    }

    @Transient
    @XmlTransient
    @JsonIgnore
    @BeanProperty var bodyHash: Int = 1

    @Transient
    @XmlTransient
    private var delivery: Delivery = null

    def getDelivery(): Delivery = delivery

    def setDelivery(d: Delivery) {
        delivery = d
        setMessageBody(delivery.getBody)
    }

    @Basic
    @BeanProperty
    var failTimes: Long = 0

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @BeanProperty
    var id: Long = -1

    @Column(name = "msg_content", length = 10000)
    @Lob
    var messageBody: Array[Byte] = new Array[Byte](0)

    def getMessageBody(): Array[Byte] = messageBody

    def setMessageBody(messageBody: Array[Byte]) {
        this.messageBody = messageBody
        this.bodyHash = Arrays.hashCode(messageBody)
    }

    @ManyToOne(fetch = FetchType.EAGER, cascade = Array(CascadeType.REFRESH))
    @JoinColumn(name = "queue_cfg_id")
    @XmlTransient
    @BeanProperty
    var queueCfg: QueueCfg = null

    @Basic
    @BeanProperty
    var timestamp: Long = System.currentTimeMillis

    @Embedded
    @BeanProperty
    var response: MsgResp = null

    def fail: Long = {
        this.timestamp = System.currentTimeMillis
        failTimes += 1
        return failTimes
    }

    @Transient
    def isExceedFailTimes: Boolean = {
        if (queueCfg != null) {
            val retryLimit = queueCfg.getRetryLimit
            return failTimes >= (if (retryLimit < 0) DEFAULT_RETRY_LIMIT else retryLimit)
        }
        return failTimes > DEFAULT_RETRY_LIMIT
    }

    @Transient
    def isSucceeded: Boolean = response != null && StringUtils.equalsIgnoreCase(response.getResponseStr, "ok")

    override def clone: MessageContext = {
        val mc = new MessageContext
        mc.id = id
        mc.bodyHash = bodyHash
        mc.delivery = MessageContext.clone(delivery)
        mc.failTimes = failTimes
        mc.messageBody = messageBody
        mc.queueCfg = queueCfg
        mc.timestamp = timestamp
        mc.response = response.clone
        mc
    }


    def canEqual(other: Any): Boolean = other.isInstanceOf[MessageContext]

    override def equals(other: Any): Boolean = other match {
        case that: MessageContext =>
            (that canEqual this) && {
                if (id == that.id && id != 0) true
                else if (this.queueCfg != that.queueCfg) false
                else delivery == that.delivery
            }
        case _ => false
    }

    override def hashCode(): Int = {
        if (id >= 0) return id.toInt;

        var hashCode: Int = 31 * id.toInt
        if (queueCfg != null)
            hashCode += 31 * queueCfg.hashCode()
        if (delivery != null)
            hashCode += 31 * delivery.hashCode()
        return hashCode
    }
}
