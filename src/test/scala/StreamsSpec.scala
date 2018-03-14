import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.testkit.{TestKit, TestKitBase}
import akka.util.ByteString
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures

import scala.collection.immutable.Seq
import scala.concurrent.Future

/**
  * Created by erik on 7/17/16.
  */
class StreamsSpec
    extends WordSpec
    with TestKitBase
    with ScalaFutures
    with MustMatchers {
  implicit lazy val system = ActorSystem("test")
  implicit lazy val mat = ActorMaterializer()
  "the akka stream" should {
    "be able to seperate prefix and tail" in {
      val source = Source.fromIterator(() => Range(0, 100).iterator)
      val prefixAndTail =
        source.prefixAndTail(5).runWith(Sink.head).futureValue
      prefixAndTail._1.length mustEqual 5
      prefixAndTail._2
        .grouped(100)
        .runWith(Sink.head)
        .futureValue
        .length mustEqual 95

    }
    "be able to use splitafter" ignore {
      val source = Source.fromIterator(() => Range(0, 10).iterator)
      val splitAfterStream = source
        .splitAfter(_ < 5)
        .grouped(1000)
        .mergeSubstreams
        .grouped(10)
        .runWith(Sink.head)
        .futureValue
      //      splitAfterStream.length mustEqual 5
      //      splitAfterStream._2.grouped(100).runWith(Sink.head).futureValue.length mustEqual 95
      splitAfterStream mustEqual false

    }

    "be able to use splitWhen" ignore {
      val source = Source.fromIterator(() => Range(0, 10).iterator)
      val splitWhenStream = source
        .splitWhen(_ == 5)
        .grouped(1000)
        .concatSubstreams
        .grouped(10)
        .runWith(Sink.head)
        .futureValue
      splitWhenStream mustEqual 0

    }

    "be able to use take" in {
      val source = Source.fromIterator(() => Range(0, 10).iterator)
      source.take(2).runWith(Sink.seq).futureValue mustEqual Seq(0, 1)
      // doesn't work, it recreates the source
//      source.take(2).runWith(Sink.seq).futureValue mustEqual Seq(2, 3)
    }

    "be able to use takeWhile" ignore {
      val source = Source.fromIterator(() => Range(0, 10).iterator)
      val stream =
        source.takeWhile(_ <= 5).grouped(10).runWith(Sink.head).futureValue
      stream mustEqual 0

    }
    "be able to use splitWhen on a text file" ignore {
      val lines =
        """#Header1
        |#Header2
        |#Header3
        |Data1
        |Data2
        |Data3
        |Data4
        |Data5""".stripMargin
      val source = Source.fromIterator(() => lines.lines)
      val stream = source
        .scan((false, false, "unused")) {
          case ((isBoundary, prev, previousValue), v) =>
            val cur = !v.startsWith("#")
            val boundarySent = prev != cur
            (boundarySent, cur, v)
        }
        .drop(1)
        .splitWhen(_._1)
        .map(_._3)
        .grouped(1000)
        .concatSubstreams
        .grouped(10)
        .runWith(Sink.head)
        .futureValue
      stream mustEqual 0

    }
    "be able to broadcast two streams from a text file" ignore {
      val headerSink = Sink.seq[String]
      val bodySink = Sink.seq[String]
      val combinedSink =
        Sink.combine(headerSink, bodySink)(Broadcast[String](_))
      val lines =
        """#Header1
          |#Header2
          |#Header3
          |Data1
          |Data2
          |Data3
          |Data4
          |Data5""".stripMargin
      val source = Source.fromIterator(() => lines.lines)


      val result = source.runWith(combinedSink)
      result mustEqual 0
    }
    "be able to broadcast and filter headers and body in separate streams" in {
      val topHeadSink = Sink.seq[String]
      val bottomHeadSink = Sink.seq[String]
      val headerFilter = Flow[String].takeWhile(_.startsWith("#"))
      val bodyFilter = Flow[String].dropWhile(_.startsWith("#"))
      val lines =
        """#Header1
          |#Header2
          |#Header3
          |Data1
          |Data2
          |Data3
          |Data4
          |Data5""".stripMargin
      val source = Source.fromIterator(() => lines.lines)

      val graph = RunnableGraph.fromGraph(GraphDSL.create(topHeadSink, bottomHeadSink)((_, _)) { implicit builder =>
        (topHS, bottomHS) =>
          import GraphDSL.Implicits._
          val broadcast = builder.add(Broadcast[String](2))
          source ~> broadcast.in
          broadcast.out(0) ~> headerFilter ~> topHS.in
          broadcast.out(1) ~> bodyFilter ~> bottomHS.in
          ClosedShape
      })
      
      val (headersFut, bodyFut) = graph.run()
      headersFut.futureValue mustEqual Vector("#Header1", "#Header2", "#Header3")
      bodyFut.futureValue mustEqual Vector("Data1", "Data2", "Data3", "Data4", "Data5")
    }
  }
  "case classes" should {
    case class A(a: String, b: Int)
    "be able to unpack from a tuple" in {
      val x = ("aString", 1)
      val y = ("bString", 2)
      val tuples = Seq(x, y)
      tuples.map(A.tupled)

    }
  }
  "work with a bytestring" in {
    ByteString(ByteString("blah").head)
    val b = ByteString("ihatebytes")
    val x = b.foldLeft(ByteString.empty)(_ ++ ByteString(_)) mustEqual b
    //scala.io.Source.fromInputStream(x)
    pending
  }
}
