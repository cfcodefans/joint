import java.util
import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import akka.actor.{ActorRef, ActorSystem, FSM, Props}
import org.apache.commons.collections4.CollectionUtils
import org.apache.logging.log4j.{LogManager, Logger}
import org.junit.Test

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by fan on 2017/2/16.
  */
object ActorTests {
    val log: Logger = LogManager.getLogger(classOf[ActorTests])

    sealed trait State

    case object Idle extends State

    case object Active extends State

    class BatchActor[M <: java.io.Serializable](val batchSize: Int = 10,
                                                val batch: util.LinkedList[M] = new util.LinkedList[M]()) extends FSM[State, util.Collection[M]] {
        startWith(Idle, batch, Option(1.millisecond))

        onTransition({
            case (transition: (ActorTests.State, ActorTests.State)) => log.info(s"${transition._1} => ${transition._2}")
        })

        def processBatch(): Unit = {
            if (CollectionUtils.isEmpty(batch)) return
            log.info(batch.asScala.mkString("\n\t", ", ", "\n"))
            batch.clear()
        }

        when(Idle, 50.millisecond) {
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

        when(Active, 50.millisecond) {
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

    val executor: ExecutorService = Executors.newSingleThreadExecutor()
}

class ActorTests {

    import ActorTests._

    @Test def testBatch(): Unit = {
        val sys: ActorSystem = ActorSystem(name = "test", defaultExecutionContext = Option(ExecutionContext.fromExecutorService(executor)))
        val ar: ActorRef = sys.actorOf(Props[BatchActor[String]](new BatchActor[String]()))

        for (i <- 1 to 1000) {
            ar ! "test\t" + i
            if (i % 15 == 0) {
                Thread.sleep(100)
            }
        }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
    }
}
