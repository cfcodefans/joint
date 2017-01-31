package org.joint.amqp.entity

import javax.persistence.{Basic, Column}

import scala.beans.BeanProperty

/**
  * Created by fan on 2017/1/30.
  */
object MsgResp {
    val FAILED: Int = 0
}

class MsgResp extends Serializable with Cloneable {
    import MsgResp._

    @Basic
    @BeanProperty
    var statusCode = 200
    
    @Basic
    @Column(name = "responseStr", columnDefinition = "TEXT", nullable = true) //mysql
    @BeanProperty
    var responseStr: String = "ok"

    override def toString = s"MsgResp($statusCode, $responseStr)"


    def canEqual(other: Any): Boolean = other.isInstanceOf[MsgResp]

    override def equals(other: Any): Boolean = other match {
        case that: MsgResp =>
            (that canEqual this) &&
                statusCode == that.statusCode &&
                responseStr == that.responseStr
        case _ => false
    }

    override def hashCode(): Int = {
        val state = Seq(statusCode, responseStr)
        state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }
}
