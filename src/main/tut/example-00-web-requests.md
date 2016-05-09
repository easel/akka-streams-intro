### Problem: Mirror Stack OverFlow

What could be easier?

```scala
def getUrlContent(url: String) = Http(url).asString.body

val FirstQuestionId = 4
val LastQuestionId = 37120353

val ids = Range(FirstQuestionId, LastQuestionId)

// URL's are always http://stackoverflow.com/questions/$i.
val urls = ids.map(id => s"http://stackoverflow.com/questions/$id")

urls.take(10).map(getUrlContent).toList.map(_.length))
```


## Some utility functions

```tut
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


# And the imports and implicits

```tut:silent
import scala.concurrent.duration._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scalaj.http._
```



# Noooooo!

```java
java.lang.OutOfMemoryError: Java heap space
  at java.lang.AbstractStringBuilder.<init>(AbstractStringBuilder.java:68)
  at java.lang.StringBuilder.<init>(StringBuilder.java:112)
  at scala.StringContext.standardInterpolator(StringContext.scala:123)
  at scala.StringContext.s(StringContext.scala:95)
  at $anonfun$1.apply(<console>:13)
  at $anonfun$1.apply(<console>:13)
  at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:234)
  at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:234)
  at scala.collection.immutable.Range.foreach(Range.scala:160)
  at scala.collection.TraversableLike$class.map(TraversableLike.scala:234)
  at scala.collection.AbstractTraversable.map(Traversable.scala:104)
  ... 21 elided
```

### What happened?

Scala attempted to materialize the entire list in memory.



## Lazyness to the Rescue

### Iterate all the Things!
```tut
def getUrlContent(url: String) = Http(url).asString.body

val idsIter = Range(4, 37120353).iterator

val urls = idsIter.map(id => s"http://stackoverflow.com/questions/$id")

urls.next()

ptime(urls.take(10).map(getUrlContent).toList.map(_.length))

```



### What if we need to throttle?
```tut:silent
def getUrlContentThrottled(url: String) = {
    Thread.sleep(100)
    Http(url).asString.body
}
```
```tut
ptime(urls.take(10).map(getUrlContentThrottled).toList.map(_.length))
```



### What if we need parallelize?
```tut
def getAsync(url: String) = Future(getUrlContentThrottled(url))
lazy val futures = urls.take(10).toList.map(getAsync)
lazy val futureSeq = Future.sequence(futures)
ptime(Await.result(futureSeq, 10.seconds).map(_.length))
```

-----------

- Are we still throttling?
- What if we need to make 1,000,000 calls, 10 at a time??
- What happens if we need to make 10,000 calls concurrently?
- What happens if we need to retry?
- What happens if the upstream server is really slow?



# Eliminating Blocking

- 10,000 concurrent calls
    - Requires 10,000 threads
    - With 1m stack, 10gb of RAM before heap.
- Naive throttling waits the initial delay and then destroys the upstream server.
- Other negative side effects notwithstanding.
- Net-Net, we need to get rid of the `Await.result`

