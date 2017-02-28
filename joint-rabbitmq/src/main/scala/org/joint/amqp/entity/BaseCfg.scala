package org.joint.amqp.entity

import javax.persistence.Basic

import scala.beans.BooleanBeanProperty

/**
  * Created by fan on 2017/2/28.
  */
class BaseCfg extends BaseEntity {
    @Basic
    @BooleanBeanProperty
    var enabled = true
}
