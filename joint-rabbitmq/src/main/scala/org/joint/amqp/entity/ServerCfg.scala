package org.joint.amqp.entity

import java.io.Serializable
import java.net.{URI, URLEncoder}
import java.util.function._
import javax.persistence._
import javax.xml.bind.annotation.XmlRootElement

import scala.beans.BeanProperty

/**
  * Created by chenf on 2016/12/21.
  */
@XmlRootElement
@Entity
@Table(name = "server_cfg")
@Cacheable(false)
class ServerCfg extends BaseEntity with Serializable with Supplier[URI] with (() => URI) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @BeanProperty
    var id: Int = -1

    @BeanProperty
    var host: String = "localhost"

    @BeanProperty
    var port: Int = 5672

    @BeanProperty
    var username: String = "guest"

    @BeanProperty
    var password: String = "guest"

    @BeanProperty
    var virtualHost: String = "/"

    @BeanProperty
    var logFilePath: String = null

    @BeanProperty
    var logFileSize: String = "2GB"

    override def toString = s"ServerCfg(id=$id, host=$host, port=$port, username=$username, password=$password, virtualHost=$virtualHost, logFilePath=$logFilePath)"

    def canEqual(other: Any): Boolean = other.isInstanceOf[ServerCfg]

    override def equals(other: Any): Boolean = other match {
        case that: ServerCfg => {
            if (this.id != -1 && this.getId == that.getId) true

            (that canEqual this) &&
                host == that.host &&
                port == that.port &&
                username == that.username &&
                password == that.password &&
                virtualHost == that.virtualHost
        }
        case _ => false
    }


    override def hashCode: Int = {
        val prime = 31
        var result = super.hashCode
        result = prime * result + (if (id == null) 0 else id.hashCode)
        if (id != -1) return result

        result = prime * result + (if (host == null) 0 else host.hashCode)
        result = prime * result + port
        result = prime * result + (if (username == null) 0 else username.hashCode)
        result = prime * result + (if (virtualHost == null) 0 else virtualHost.hashCode)
        result
    }

    override def get(): URI = new URI(s"amqp://$username:$password@$host:$port/${URLEncoder.encode(virtualHost, "US-ASCII")}")

    override def apply() = get()
}
