package org.joint.amqp.config

import java.io.Serializable
import javax.persistence._
import javax.xml.bind.annotation.{XmlRootElement, XmlTransient}

import com.fasterxml.jackson.annotation.JsonIgnore
import com.rabbitmq.client.QueueingConsumer.Delivery
import org.apache.commons.lang3.StringUtils

import scala.beans.BeanProperty

object MessageContext {
    val DEFAULT_RETRY_LIMIT: Int = 100
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

    @Transient
    @XmlTransient
    @JsonIgnore
    @BeanProperty var bodyHash: Int = 1

    @Transient
    @XmlTransient
    @BeanProperty
    var delivery: Delivery = null

    @Basic
    @BeanProperty
    var failTimes: Long = 0

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @BeanProperty
    var id: Long = -1

    @Column(name = "msg_content", length = 10000)
    @Lob
    @BeanProperty
    var messageBody: Array[Byte] = new Array[Byte](0)

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
        failTimes += 1;
        return failTimes
    }

    @Transient def isExceedFailTimes: Boolean = {
        if (queueCfg != null) {
            val retryLimit = queueCfg.getRetryLimit
            return failTimes >= (if (retryLimit < 0) DEFAULT_RETRY_LIMIT else retryLimit)
        }
        return failTimes > DEFAULT_RETRY_LIMIT
    }

    @Transient def isSucceeded: Boolean = response != null && StringUtils.equalsIgnoreCase(response.getResponseStr, "ok")
}
