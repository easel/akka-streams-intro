# Actor Model

- Eliminates Explicit Locking and Thread Management
- Encapsulated
- Location Agnostic
- Communicate Exclusively via Messages
- Thread-safe within Receive Method
- Single receive method


# Imports and implicits

```tut:silent
import scala.concurrent.duration._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._, akka.stream._, akka.stream.scaladsl._, scalaj.http._

```


## Some utility functions

```tut
val idsIter = Range(4, 37120353).iterator

val urls = idsIter.map(id => s"http://stackoverflow.com/questions/$id")

def getUrlContent(url: String) = Http(url).asString.body

def getUrlContentThrottled(url: String) = {
    Thread.sleep(100)
    Http(url).asString.body
}

def getAsync(url: String) = Future(getUrlContentThrottled(url))

/**
 * Time a function call returning a tuple of the elapsed time and the result
 */
def ptime[A](f: => A): (String, A) = {
  val t0 = System.nanoTime
  val ans = f
  val elapsed = f"${((System.nanoTime-t0)*1e-9)}%.3f sec"
  (elapsed, ans)
}

ptime("My function result")
```




```scala
object SpiderActor {
  sealed abstract class Message
  case class Request(url: String) extends Message
  case class Response(body: String) extends Message

  def getUrlContent(url: String): Future[Response] = Future{
    Response(Http(url).asString.body)
  }

  val ThrottleDelay = 100.milliseconds
  val MaxInFlight = 10

  def run(urls: Iterator[String]): Future[Seq[Int]] = {
    val promise = Promise[Seq[Int]]
    var lengths = Seq.empty[Int]

    def persist(body: String) = Future.successful {
      lengths :+= body.length
    }

    val system = ActorSystem()

    println("Starting SpiderActor")
    val actorRef = system.actorOf(Props(new SpiderActor(urls, persist)), "spider")
    println("SpiderActor Started")

    system.whenTerminated.map { x =>
      println("SpiderActor Shut Down")
      promise.complete(Try(lengths))
    }
    promise.future
  }
}
```


```scala
class SpiderActor(urls: Iterator[String], persist: (String) => Future[Unit]) extends Actor {
  import SpiderActor._

  var inFlight = 0

  private def nextRequest() = {
    if(inFlight < MaxInFlight && urls.nonEmpty) {
      inFlight += 1
      context.system.scheduler.scheduleOnce(ThrottleDelay, self, Request(urls.next))
    }
    else if (inFlight == 0 && urls.isEmpty) self ! PoisonPill
  }

  nextRequest()

  def receive: Receive = {
    case Request(url) =>
      nextRequest()
      getUrlContent(url).pipeTo(self)
    case Response(body: String) =>
      inFlight -= 1
      persist(body).map(_ => nextRequest())
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    super.postStop()
    context.system.terminate()
  }
}
```


```tut
import com.github.easel._
val u = urls.take(10)
ptime(Await.result(SpiderActor.run(u), 10.seconds))
```
