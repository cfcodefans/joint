import java.util

import akka.actor.FSM
import org.apache.commons.collections4.CollectionUtils
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
  * Created by fan on 2017/2/16.
  */
object ActorTests {
    val log: Logger = LogManager.getLogger(classOf[ActorTests])

    sealed trait State

    case object Idle extends State

    case object Active extends State

    class BatchActor[M <: java.io.Serializable](val batchSize: Int = 100,
                                                val batch: util.LinkedList[M] = new util.LinkedList[M]()) extends FSM[State, M] {
        startWith(Idle, null.asInstanceOf[M], Option(1.millisecond))

        onTransition({
            case (transition: (State, State)) => log.info(s"${transition._1} => ${transition._2}")
        })

        def processBatch(): Unit = {
            if (CollectionUtils.isEmpty(batch)) return
            log.info(batch.asScala.mkString("\n\t", "\n\t", "\n"))
        }

        when(Idle, 500.millisecond) {
            case ev@Event(StateTimeout, m: M) => {
                processBatch()
                goto(Idle)
            }
            case ev@Event(msg: M, m: M) => {

            }

        }

    }

}

class ActorTests {

}
