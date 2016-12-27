package org.joint.amqp.config

import java.io.Serializable
import javax.persistence.{Cacheable, Entity, Table}

import scala.beans.BeanProperty

/**
  * Created by chenf on 2016/12/21.
  */
@Entity
@Table(name = "server_cfg")
@Cacheable(false)
class ServerCfg extends Serializable {

    @BeanProperty
    var host: String = "localhost"

}
