package com.github.easel

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by erik on 5/9/16.
  */
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

  def main(args: Array[String]): Unit = {
    val urls = Seq("http://google.com", "http://yahoo.com")
    val result = Await.result(run(urls, (x) => Future.successful(x)), 10.seconds)
    println(result)
  }

}
