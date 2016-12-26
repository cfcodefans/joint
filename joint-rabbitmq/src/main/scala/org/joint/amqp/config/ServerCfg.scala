package org.joint.amqp.config

import java.io.Serializable
import javax.jdo.annotations.{Cacheable, PersistenceCapable}

/**
  * Created by chenf on 2016/12/21.
  */
@PersistenceCapable
@Cacheable(false.toString)
class ServerCfg extends Serializable {

}
