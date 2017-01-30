import java.util.concurrent.{ExecutorService, Executors}

import com.rabbitmq.client.impl.ForgivingExceptionHandler
import com.rabbitmq.client.{Channel, Connection, ConnectionFactory}
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.scala.ScalaMiscs._
import org.junit.{AfterClass, Test}

object RabbitMQTests {
    val log: Logger = LogManager.getLogger(classOf[RabbitMQTests])

    val AMQP_URL: String = "amqp://poppen:poppen@snowball:5672/%2Fpoppen"

    val sharedExecutor: ExecutorService = Executors.newWorkStealingPool()

    lazy val factory: ConnectionFactory = {
        val f = new ConnectionFactory()
        f.setUri(AMQP_URL: String)
        f.setSharedExecutor(sharedExecutor)
        f.setExceptionHandler(new ForgivingExceptionHandler)
        log.info(s"ConnectionFactory($AMQP_URL))")
        f
    }

    @AfterClass
    def tearDown(): Unit = {
        sharedExecutor.shutdownNow()
    }
}

class RabbitMQTests {

    import RabbitMQTests._

    @Test
    def testConnection(): Unit = {
        try_(factory.newConnection)((conn: Connection) => {
            log.info(s"connection.getId = ${conn.getId}")
        })
    }

    @Test
    def testChannel(): Unit = {
        try_(factory.newConnection)((conn: Connection) => {
            try_(conn.createChannel)((ch: Channel) => {
                log.info(ch)
                log.info(ch.messageCount("text_mail"))
            })
        })
    }

    @Test
    def testExchange(): Unit = {
        try_(factory.newConnection)((conn: Connection) => {
            try_(conn.createChannel)((ch: Channel) => {
                log.info(ch)
            })
        })
    }
}