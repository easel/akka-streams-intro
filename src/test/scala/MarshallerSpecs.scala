import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}

class MarshallerSpecs extends WordSpec with MustMatchers with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val mat: Materializer = ActorMaterializer()
  implicit val unmarshaller: Unmarshaller[HttpEntity, Int] =
    Unmarshaller.byteArrayUnmarshaller.map { byteArray =>
      byteArray.length
    }

  val httpEntity = HttpEntity("foo")
  "the unmarshaller" should {
    "work" in {
      Unmarshal(httpEntity).to[Int].futureValue mustEqual 3
    }
  }
  "do stuff" should {
    "use streams" in {

//      def recursiveFlow = Flow[String].
      Source
        .single("a")
        .mapAsync
        .flatMapMerge(10, s => Source.single(s))
        .runWith(Sink.seq) mustEqual Seq("a", "a")

    }
    "merge streams" in {
      Source.single("a").merge(Source.single("b")).runWith(Sink.seq).futureValue mustEqual Seq("a", "b")
    }
  }
}
