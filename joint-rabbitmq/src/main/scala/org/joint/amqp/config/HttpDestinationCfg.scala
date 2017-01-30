package org.joint.amqp.config

import java.net.URI
import java.util.Objects
import java.util.function.Supplier
import javax.persistence._
import javax.xml.bind.annotation.XmlRootElement

import scala.beans.BeanProperty

/**
  * Created by fan on 2017/1/30.
  */
@XmlRootElement
@Entity
@Table(name = "dest_cfg")
@Cacheable(true)
class HttpDestinationCfg extends BaseEntity with Supplier[URI] with Function0[URI] {
    @Basic
    @BeanProperty
    var hostHead: String = null

    @Basic
    @BeanProperty
    var httpMethod: String = "post"

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @BeanProperty
    var id: Int = -1

    @Basic
    @BeanProperty
    var timeout: Long = 30000

    @Basic
    @BeanProperty
    var url: String = null

    override def get(): URI = new URI(url)

    override def apply(): URI = get()

    override def equals(obj: Any): Boolean = {
        if (this eq obj.asInstanceOf[AnyRef]) return true

        if (!obj.isInstanceOf[HttpDestinationCfg]) return false

        val other: HttpDestinationCfg = obj.asInstanceOf[HttpDestinationCfg]

        if (id != -1) return id == other.id

        if (!Objects.equals(httpMethod, other.httpMethod)) return false
        if (!Objects.equals(url, other.url)) return false

        return true
    }

    override def hashCode: Int = {
        val prime = 31
        var result = super.hashCode
        result = prime * result + id
        if (id != -1) return id

        result = prime * result + (if (httpMethod == null) 0 else httpMethod.hashCode)
        result = prime * result + (if (url == null) 0 else url.hashCode)
        result
    }

    override def toString = s"HttpDestinationCfg(hostHead=$hostHead, httpMethod=$httpMethod, id=$id, timeout=$timeout, url=$url)"
}
