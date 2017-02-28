package org.joint.amqp.entity

import java.util.Objects
import javax.persistence._
import javax.xml.bind.annotation.{XmlRootElement, XmlTransient}

import com.rabbitmq.client.BuiltinExchangeType

import scala.beans.{BeanProperty, BooleanBeanProperty}

/**
  * Created by fan on 2017/1/30.
  */
@XmlRootElement
@Entity
@Table(name = "exchange_cfg")
@Cacheable(false)
class ExchangeCfg extends BaseCfg {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @BeanProperty
    var id: Int = -1

    @BeanProperty
    @Column(name="exchangename")
    var name: String = _

    //    @Enumerated(EnumType.STRING)
    @BeanProperty
    @Column(name="type")
    var exchangeType: String = BuiltinExchangeType.DIRECT.name()

    @BooleanBeanProperty
    var durable: Boolean = false

    @BooleanBeanProperty
    var autoDelete: Boolean = false

    @BooleanBeanProperty
    @Transient //TODO add later
    @XmlTransient
    var internal: Boolean = false

    //TODO @BeanProperty var arguments: util.Map[String, AnyRef] = new util.HashMap[String, AnyRef]()

    @ManyToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.PERSIST, CascadeType.REFRESH))
    var queues: java.util.Set[QueueCfg] = new java.util.HashSet[QueueCfg]

    @BeanProperty
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "server_id")
    var server: ServerCfg = _

    override def hashCode: Int = {
        val prime = 31
        var result = super.hashCode
        if (id != -1) return id.toInt
        result = prime * result + (if (name == null) 0 else name.hashCode)
        result = prime * result + (if (server == null) 0 else server.hashCode)
        result
    }

    override def equals(obj: Any): Boolean = {
        if (this eq obj.asInstanceOf[AnyRef]) return true
        if (!obj.isInstanceOf[ExchangeCfg]) return false

        val other = obj.asInstanceOf[ExchangeCfg]
        if (id != -1) return id == other.id

        if (!Objects.equals(name, other.name)) return false
        if (!Objects.equals(server, other.server)) return false

        true
    }

    override def toString = s"ExchangeCfg($id, $name, $exchangeType, $durable, $autoDelete, $internal, $server)"
}
