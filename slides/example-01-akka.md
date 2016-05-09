# Actor Model

- Eliminates Explicit Locking and Thread Management
- Encapsulated
- Location Agnostic
- Communicate Exclusively via Messages
- Thread-safe within Receive Method
- Single receive method


# Imports and implicits

```scala
import scala.concurrent.duration._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._, akka.stream._, akka.stream.scaladsl._, scalaj.http._

```


## Some utility functions

```scala
     | val idsIter = Range(4, 37120353).iterator
idsIter: Iterator[Int] = non-empty iterator

scala> val urls = idsIter.map(id => s"http://stackoverflow.com/questions/$id")
urls: Iterator[String] = non-empty iterator

scala> def getUrlContent(url: String) = Http(url).asString.body
getUrlContent: (url: String)String

scala> def getUrlContentThrottled(url: String) = {
     |     Thread.sleep(100)
     |     Http(url).asString.body
     | }
getUrlContentThrottled: (url: String)String

scala> def getAsync(url: String) = Future(getUrlContentThrottled(url))
getAsync: (url: String)scala.concurrent.Future[String]

scala> /**
     |  * Time a function call returning a tuple of the elapsed time and the result
     |  */
     | def ptime[A](f: => A): (String, A) = {
     |   val t0 = System.nanoTime
     |   val ans = f
     |   val elapsed = f"${((System.nanoTime-t0)*1e-9)}%.3f sec"
     |   (elapsed, ans)
     | }
ptime: [A](f: => A)(String, A)

scala> ptime("My function result")
res2: (String, String) = (0.000 sec,My function result)
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


```scala
scala> import com.github.easel._
import com.github.easel._

scala> val u = urls.take(10)
u: Iterator[String] = non-empty iterator

scala> ptime(Await.result(SpiderActor.run(u), 10.seconds))
Starting SpiderActor
SpiderActor Started
res3: (String, Seq[Int]) = (0.950 sec,List(191, 195, 210, 195, 166, 166, 172, 171, 177, 157))
```
