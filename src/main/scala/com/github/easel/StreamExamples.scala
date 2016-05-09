package com.github.easel
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, DelayOverflowStrategy, OverflowStrategy, ThrottleMode}
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by erik on 5/11/16.
  */
object StreamExamples {
  val source: Source[Int, NotUsed] = Source(0 to 100)
  val flow: Flow[Int, String, NotUsed] = Flow[Int].map(_.toString)
  val sink: Sink[Any, Future[Done]] = Sink.foreach(println)

  val exampleFlow = flow
    .buffer(10, OverflowStrategy.backpressure) // buffer and apply backpressure
    .buffer(10, OverflowStrategy.dropTail) // buffer and drop old
    .delay(10.millis, DelayOverflowStrategy.backpressure) // force a delay downstream
    .throttle(1, 10.millis, 10, ThrottleMode.Shaping) // throttle to 1 in 10 milliseconds
    .map(_.toString) // map to a new type
    .async // introduce an async boundary
    .grouped(2) // convert every pair of ints to a Seq(1, 2)
    .mapConcat(identity) // expand the groups back to ints
    .mapAsyncUnordered(10)(Future.successful) // do 10 things asynchronously
    .intersperse(",") // intersperse "," similar to mkString

  def main(args: Array[String]) = {
    implicit lazy val actorSystem = ActorSystem()
    implicit lazy val materializer = ActorMaterializer()
    val result = source.via(flow).runWith(sink)
    Await.result(result, 1.seconds)
  }

}
