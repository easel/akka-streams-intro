# Akka Streams
## Source -> Flow -> Sink
```scala
import scala.concurrent.duration._, scala.concurrent._,  scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._, akka.stream._, akka.stream.scaladsl._, scalaj.http._, com.github.easel._
val source: Source[Int, akka.NotUsed] = Source(0 to 3)
val flow: Flow[Int, String, akka.NotUsed] = Flow[Int].map(_.toString)
val sink: Sink[Any, Future[akka.Done]] = Sink.foreach(println)
```
```scala
scala> def run = {
     |   implicit val actorSystem = ActorSystem()
     |   implicit val materializer = ActorMaterializer()
     |   val result = source.via(flow).runWith(sink)
     |   Await.result(result, 1.seconds)
     |   actorSystem.terminate()
     | }
run: scala.concurrent.Future[akka.actor.Terminated]

scala> run
0
1
2
3
res0: scala.concurrent.Future[akka.actor.Terminated] = List()
```


## Some utility functions

```scala
scala> val idsIter = Range(4, 37120353).iterator
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
object SpiderStream {
  val Concurrency = 10
  val BatchSize = 100

  def source(args: Iterator[String]) = Source.fromIterator(() => args)

  def flow(f: (String) => Future[String]) = Flow[String]
    .throttle(1, 100.millis, 1, ThrottleMode.shaping)
    .mapAsync(Concurrency)(f)
    .map(_.length)
    .grouped(BatchSize)

  def run(args: Seq[String], f: (String) => Future[String]): Future[Seq[Int]] = {
    println("Starting")
    implicit val system = ActorSystem()
    system.whenTerminated.map { x =>
      println("Shut Down")
    }
    implicit val mat: Materializer = ActorMaterializer()

    val result = source(args.iterator)
      .via(flow(f))
      .runWith(Sink.head)

    result.map { x =>
      system.terminate()
      x
    }(system.dispatcher)
  }
}
```

```scala
scala> ptime((SpiderStream.run(urls.take(10).toSeq, (x) => getAsync(x)), 10.seconds))
Starting
res3: (String, (scala.concurrent.Future[Seq[Int]], scala.concurrent.duration.FiniteDuration)) = (0.039 sec,(List(),10 seconds))
```
