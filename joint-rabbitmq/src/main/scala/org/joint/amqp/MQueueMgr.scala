package org.joint.amqp

import com.rabbitmq.client.ConnectionFactory
import org.joint.amqp.config.ServerCfg

/**
  * Created by chenf on 2017/1/6.
  */
object MQueueMgr {

    def connFactory(sc: ServerCfg): ConnectionFactory = {
        if (sc == null) return null
        new ConnectionFactory()
    }
}
