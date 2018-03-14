import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{
  Broadcast,
  Flow,
  GraphDSL,
  Merge,
  MergeSorted,
  RunnableGraph,
  Sink,
  Source
}
import akka.testkit.TestKit
import org.scalatest.{AsyncWordSpecLike, MustMatchers}

import scala.concurrent.Future

/**
  * Created by erik on 9/6/16.
  */
class DedupeMergeSpec
    extends TestKit(ActorSystem("test"))
    with AsyncWordSpecLike
    with MustMatchers {
  implicit val mat = ActorMaterializer()

  val seqA = Seq(0, 1, 2, 5, 9, 10)
  lazy val sourceA = Source.fromIterator(() => seqA.iterator)
  val seqB = Seq(2, 4, 7)
  lazy val sourceB = Source.fromIterator(() => seqB.iterator)
  val seqC = Seq(2, 6, 9)
  lazy val sourceC = Source.fromIterator(() => seqC.iterator)
  val seqD = Seq(2, 6, 9, 11, 12)
  lazy val sourceD = Source.fromIterator(() => seqD.iterator)

  val out = Sink.seq[Int]

  val g = RunnableGraph.fromGraph(GraphDSL.create(out) {
    implicit builder => sink =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))

      sourceA ~> merge
      sourceB ~> merge ~> sink
      ClosedShape
  })

  def dedupeSortedFlow[T]: Flow[T, T, NotUsed] =
    Flow[T]
      .scan((Option.empty[T], Option.empty[T])) {
        //
        case ((None, None), input) ⇒
          // first record
          (Option(input), None)

        case ((Some(prev), None), input) ⇒
          if (prev == input) {
            // first duplicate
            (None, Option(input))
          } else {
            // normal records
            (Option(input), None)
          }

        case ((None, Some(prev)), input) ⇒
          if (prev == input) {
            // consequent duplicates
            (None, Option(input))
          } else {
            // new record
            (Option(input), None)
          }
      }
      .collect { case (Some(item), _) ⇒ item }
  "merge and dedupe" should {
    "work via the flow dsl" in {
      sourceA
        .mergeSorted(sourceB)
        .via(dedupeSortedFlow)
        .runWith(Sink.seq)
        .map { results =>
          results mustEqual Seq(0, 1, 2, 4, 5, 7, 9, 10)
        }
    }
    "fold-merge from a list of sources via the flow dsl" in {
      val sources = Seq(sourceA, sourceB, sourceC, sourceD)
      sources.tail.foldLeft(sources.head)((acc, item) => acc.mergeSorted(item))
        .via(dedupeSortedFlow)
        .runWith(Sink.seq)
        .map { results =>
          results mustEqual Seq(0, 1, 2, 4, 5, 6, 7, 9, 10, 11, 12)
        }
    }
    "merge via a graph stage" ignore {
      val resultFut: Future[Seq[Int]] = g.run()
      resultFut.map { results =>
        results mustEqual Seq(0, 1, 2, 4, 5, 7, 9, 10)
      }
    }
  }

}
