import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.Source

import scala.concurrent.duration._
import kamon.Kamon
import kamon.metric.instrument.Histogram

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class Msg(met: Histogram)

class ParentActor extends Actor {
  private var counter = 0

  private val childActor1 = context.actorOf(Props[ChildActor], "childActor1")
  private val childActor2 = context.actorOf(Props[ChildActor], "childActor2")

  def receive = {
    case msg: Msg =>
      counter += 1
      if (counter % 2 == 0)
        childActor1 ! msg
      else
        childActor2 ! msg
  }
}

class ChildActor extends Actor {
  def receive = {
    case Msg(hist) =>
      hist.record(sys.runtime.freeMemory() / 1000000)
  }
}

object Main extends App {
  Kamon.start()

  val mCounter = Kamon.metrics.counter("map-counter")
  val fCounter = Kamon.metrics.counter("filter-counter")
  val freeMemoryHist = Kamon.metrics.histogram("freeMemoryHist")

  implicit val system = ActorSystem("mySystem")

  implicit val materializer =  {
    val matSetting = ActorMaterializerSettings(system).withDispatcher("my-dispatcher")
    ActorMaterializer(Some(matSetting))
  }

  try {
    val parentActor = system.actorOf(Props[ParentActor], "parentActor")

    def run(max: Int): Future[Int] = {
      Source(1 to max)
        .filter{ n =>
          fCounter.increment()
          n % 2 == 0
        }
        .map{ _ =>
          parentActor ! Msg(freeMemoryHist)
          mCounter.increment()
          1
        }
        .runFold(0) { _ + _ }
    }

    val results = Await.result(Future.traverse((1 to 80)) { _ => run(200000) }, Duration.Inf)

    results.foreach(println)

    Thread.sleep(1000 * 10)
  } finally {
    val terminate = system.terminate()
    Kamon.shutdown()

    Await.result(terminate, Duration.Inf)
  }

}