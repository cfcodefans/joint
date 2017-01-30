package org.joint.amqp.config

import java.io.Serializable
import javax.persistence.{Basic, Column}

import scala.beans.BeanProperty

/**
  * Created by fan on 2017/1/30.
  */
@SerialVersionUID(1L)
class BaseEntity extends Serializable {
    @Basic
    @Column(name = "ver")
    @BeanProperty
    var version: Int = 0

    override def hashCode: Int = {
        val prime = 31
        var result = 1
        result = prime * result + version
        result
    }

    override def equals(obj: Any): Boolean = {
        if (this eq obj.asInstanceOf[AnyRef]) return true

        if (obj == null) return false

        if (!obj.isInstanceOf[BaseEntity]) return false

        val other = obj.asInstanceOf[BaseEntity]

        if (version != other.version) return false

        return true
    }
}
