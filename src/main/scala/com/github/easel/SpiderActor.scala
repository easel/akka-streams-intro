package com.github.easel

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.pattern.pipe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.util.Try
import scalaj.http._

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

  def main(args: Array[String]): Unit = {
    val urls: Iterator[String] = Iterator("http://google.com", "http://yahoo.com")
    Await.result(run(urls), 1.minutes)
  }
}

class SpiderActor(urls: Iterator[String], persist: (String) => Future[Unit]) extends Actor {
  import SpiderActor._

  var inFlight = 0

  private def nextRequest() = {
    // println(s"nextRequest, inFlight is $inFlight")
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
