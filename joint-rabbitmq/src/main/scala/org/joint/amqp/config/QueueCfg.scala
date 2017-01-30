package org.joint.amqp.config

import java.util.Objects
import javax.persistence._
import javax.xml.bind.annotation.XmlRootElement

import org.apache.commons.lang3.StringUtils

import scala.beans.{BeanProperty, BooleanBeanProperty}

/**
  * Created by fan on 2017/1/30.
  */
@XmlRootElement
@Entity
@Table(name = "queue_cfg")
@Cacheable(false)
class QueueCfg extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @BeanProperty
    var id: Int = -1

    @Basic
    @BeanProperty
    var name: String = _

    @Basic
    @BooleanBeanProperty
    var durable: Boolean = false

    @Basic
    @BooleanBeanProperty
    var exclusive: Boolean = false

    @Basic
    @BooleanBeanProperty
    var autoDelete: Boolean = false

    @Basic
    @Column(name = "prefetch_size")
    var prefetchSize: Int = 0

    @Column(name = "retry_limit")
    @BeanProperty
    var retryLimit: Int = 0

    @BeanProperty
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "server_id")
    var server: ServerCfg = _

    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "queues", cascade = Array(CascadeType.PERSIST, CascadeType.REFRESH))
    @BeanProperty
    var exchanges: java.util.Set[ExchangeCfg] = new java.util.HashSet[ExchangeCfg]

    @Basic
    @BeanProperty
    var routeKey: String = StringUtils.EMPTY

    @Enumerated(EnumType.ORDINAL)
    @BeanProperty
    var status: Status = Status.STOPPED

    @OneToOne(fetch = FetchType.EAGER, cascade = Array(CascadeType.ALL))
    @JoinColumn(name = "dest_cfg_id")
    @BeanProperty
    var destCfg: HttpDestinationCfg = _

    override def equals(obj: Any): Boolean = {
        if (this eq obj.asInstanceOf[AnyRef]) return true

        if (!obj.isInstanceOf[QueueCfg]) return false

        val other = obj.asInstanceOf[QueueCfg]
        if (id != -1) return id == other.id

        if (!Objects.equals(name, other.name)) return false
        if (!Objects.equals(server, other.server)) return false

        return true
    }

    override def hashCode: Int = {
        val prime = 31
        var result = 1
        if (id != -1) return id.toInt

        result = prime * result + (if (name == null) 0 else name.hashCode)
        result = prime * result + (if (server == null) 0 else server.hashCode)
        return result
    }
}
