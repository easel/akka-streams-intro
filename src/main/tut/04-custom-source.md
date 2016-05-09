# Custom Source

```scala
/**
* Objects which can be identified by a key and ordered
*/
trait Keyed {
  def key: String
}

object Keyed {
  implicit def ordering[T <: Keyed]: Ordering[T] = new Ordering[T] {
    override def compare(x: T, y: T): Int = {
      x.key.compareTo(y.key)
    }
  }
}

/**
* An interface for services which provided paginated results in batches.
*/
trait BatchLoader[T <: Keyed] {
  def load(offset: Option[String]): Future[Batch[T]]

  def source(implicit ec: ExecutionContext): Source[T, ActorRef] = {
    Source.actorPublisher(BatchSource.props[T](this))
  }
}
```


```scala
class BatchSource[A <: Keyed](loader: BatchLoader[A])(implicit ec: ExecutionContext) extends ActorPublisher[A] {
  import BatchSource._

  import akka.stream.actor.ActorPublisherMessage._

  private var first = true
  private var nextOffset: Option[String] = None
  private var buffer: Seq[A] = Seq.empty

  def receive: Receive = waitingForDownstreamReq(0)

  case object Pull

  private def shouldLoadMore = {
    nextOffset.isDefined && (totalDemand > 0 || buffer.length < BUFFER_AMOUNT)
  }

  def waitingForDownstreamReq(offset: Long): Receive = {
    case Request(_) | Pull =>
      val sent = if (buffer.nonEmpty) {
        sendFromBuff(totalDemand)
      } else {
        0
      }
      if (first || (shouldLoadMore && isActive)) {
        first = false
        loader.load(nextOffset).pipeTo(self)
        context.become(waitingForFut(offset + sent, totalDemand))
      }

    case Cancel => context.stop(self)
  }

  def sendFromBuff(demand: Long): Long = {
    val consumed = buffer.take(demand.toInt).toList
    buffer = buffer.drop(consumed.length)
    consumed.foreach(onNext)
    if (nextOffset.isEmpty && buffer.isEmpty) {
      onComplete()
    }
    consumed.length.toLong
  }

  def waitingForFut(s: Long, beforeFutDemand: Long): Receive = {
    case batch: Batch[A] =>
      nextOffset = if (batch.items.isEmpty) {
        None
      } else {
        batch.nextOffset
      }
      buffer = buffer ++ batch.items
      val consumed = sendFromBuff(beforeFutDemand)
      self ! Pull
      context.become(waitingForDownstreamReq(s + consumed))

    case Request(_) | Pull => // ignoring until we receive the future response

    case Status.Failure(err) =>
      context.become(waitingForDownstreamReq(s))
      onError(err)

    case Cancel => context.stop(self)
  }
}

object BatchSource {
  final val BUFFER_AMOUNT = 1000
  def props[T <: Keyed](loader: BatchLoader[T])(implicit ec: ExecutionContext): Props = {
    Props(new BatchSource[T](loader))
  }
}
```
