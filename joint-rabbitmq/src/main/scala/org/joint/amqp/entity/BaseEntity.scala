package org.joint.amqp.entity

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
}
