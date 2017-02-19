package org.joint.akka

import java.util

import akka.actor.{ActorRef, FSM}
import org.apache.commons.collections4.CollectionUtils
import org.apache.logging.log4j.{LogManager, Logger}
import org.joint.akka.BatchActor.{Active, Idle}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
  * Created by fan on 2017/2/19.
  */
object BatchActor {
    val log: Logger = LogManager.getLogger(BatchActor.getClass)

    sealed trait State

    case object Idle extends State

    case object Active extends State

}

import org.joint.akka.BatchActor._

class BatchActor[M <: java.io.Serializable](val timeout: FiniteDuration = 1.millisecond,
                                            val batchSize: Int = 10,
                                            val batch: util.LinkedList[M] = new util.LinkedList[M]()) extends FSM[State, util.Collection[M]] {
    startWith(Idle, batch, Option(timeout))

    onTransition({
        case (transition: (BatchActor.State, BatchActor.State)) => log.info(s"${transition._1} => ${transition._2}")
    })

    def processBatch(): Unit = {
        if (CollectionUtils.isEmpty(batch)) return
        log.info(batch.asScala.mkString("\n\t", ", ", "\n"))
        batch.clear()
    }

    when(Idle, timeout) {
        case ev@Event(StateTimeout, _batch: util.Collection[M]) => {
            processBatch()
            stay()
        }
        case ev@Event(msg: M, _batch: util.Collection[M]) => {
            batch.add(msg)
            goto(Active)
        }
        case ev@_ => {
            log.info(s"not matched $ev")
            stay()
        }
    }

    when(Active, timeout) {
        case ev@Event(StateTimeout, _batch: util.Collection[M]) => {
            processBatch()
            goto(Idle).using(_batch)
        }
        case ev@Event(msg: M, _batch: util.Collection[M]) => {
            _batch.add(msg)
            if (batch.size() >= batchSize) {
                processBatch()
            }
            stay()
        }
        case ev@_ => {
            log.info(ev.toString)
            stay()
        }
    }
}
