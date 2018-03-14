import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import org.scalatest.{AsyncWordSpec, MustMatchers}
import spray.json.JsValue

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by erik on 12/12/16.
  */
class AkkaHttpSpecs extends AsyncWordSpec with MustMatchers {
  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer()
  val http = Http(system)

  val uriFile = URLEncoder
    .encode("""data+women  washington dc.eml""", "utf-8")
    .replace("+", "%20")
    .replace("%2B", "+")
  val uriStr =
    s"""https://namespace1.tenant1.hcp-demo1.hitachiconsulting.net/rest/$uriFile"""
  val rawUri = Uri(uriStr)
  val uri = rawUri.copy() //(path = rawUri.path.replace("+", "%2B"))

  "the akka http library" should {
    "encode a simple path with a : in it" ignore {
      val str = """c : d.txt"""
      val encoded = URLEncoder.encode(str, "utf-8")
      val p = Path(encoded)
      encoded mustEqual "c+%3A+d.txt"
      p.toString mustEqual str
    }
    "encode a simple path with a + in it" ignore {
      val str = """c + d.txt"""
      val encoded = URLEncoder.encode(str, "utf-8")
      val p = Path(encoded)
      encoded mustEqual "c+%2B+d.txt"
      p.toString mustEqual str
    }
    "encode a path" ignore {
      val p = Path(uriStr)
      p.toString mustEqual uriStr
    }
    "render a simple url" in {
      val str = "/a/1:2.txt"
      val uri = Uri(str)
      uri.toString mustEqual str
      uri.toHttpRequestTargetOriginForm.toString mustEqual str
    }
    "render a simple url with a plus in it" in {
      val str = "/a/1+2.txt"
      val uri = Uri(str)
      uri.toString mustEqual str
      uri.toHttpRequestTargetOriginForm.toString mustEqual str
    }
    "url encode a simple url with a plus in it" in {
      val str = "/a/1%20+2.txt"
      val uri = Uri(str)
//      uri.toString mustEqual str
      uri.toHttpRequestTargetOriginForm.toString mustEqual str
    }
    "encode a url" in {
      uri.toString mustEqual uriStr
    }
    "generate a correct path value" in {
      uri.path.toString mustEqual "/rest/" + uriFile
    }
    "generate a request url that is usable" in {
      uri.toHttpRequestTargetOriginForm.toString mustEqual "/rest/" + uriFile
    }
    "generate a request with a raw uri" in {
      import akka.http.scaladsl.model.headers.`Raw-Request-URI`
      val x = HttpRequest(uri = "/ignored",
                          headers = List(`Raw-Request-URI`("/a-b/b+d")))
      pending
    }

    "Use a source queue" in {
      val pool = Http().superPool[Unit]()
      val responseProcessorFlow= Flow[String]
        .map(uri => (HttpRequest(uri = uri), ()))
        .via(pool)
        .mapAsync(1) {
          case (Success(request), _) =>
            request.entity.toStrict(10.seconds).map(e => Some(e.data.decodeString("UTF-8")))
          case (Failure(t), _) =>
            Future.successful(Option.empty[String])
        }
        .collect { case Some(s) => s}

      println("starting test")
      //val pool = Http().newHostConnectionPool[Unit]("localhost", 9443)
      val source = Source
        .queue[String](0, OverflowStrategy.backpressure)
        .via(responseProcessorFlow)

      val sink = Sink.seq[String]
      val (sourceQueue, sinkFuture) =
        source.toMat(sink) { case (a1, b1) => a1 -> b1 }.run()

      val completionFuture = sourceQueue
        .watchCompletion()
        .map { completion =>
          println("Source completed successfully")
          completion mustEqual akka.Done
        }
        .recover {
          case t =>
            println("Source failed: t")
            t mustEqual ""
        }

      sourceQueue.offer("https://www.google.com").map { result =>
        println(s"Request queued (1) $result")
      }
      sourceQueue.offer("https://www.google.com").map { result =>
        println(s"Request queued (2) $result")
      }
      sourceQueue.offer("https://www.yahoo.com").map {
        (result: QueueOfferResult) =>
          println(s"Request queued (3) $result")
          //Thread.sleep(1000)
          //println("Completing source")
          sourceQueue.complete()

      }
      completionFuture.zip(sinkFuture).map {
        case (completion, results) =>
          results.size mustEqual 3
          println(results)
          completion
      }
    }

    "convert a bytstring to an array[byte] via toByteBuffer" in {
      val b = ByteString("MyString")
      b.toByteBuffer.array() mustEqual ""
    }

    "convert a bytstring to an array[byte] via toArray" in {
      val b = ByteString("MyString")
      b.toArray[Byte] mustEqual ""
    }

    "unmarshall response bodies" in {
      Http()
        .singleRequest(HttpRequest(uri = "http://ip.jsontest.com/"))
        .flatMap(Unmarshal(_).to[JsValue])
        .map { x =>
          x.asJsObject.fields("ip") mustBe a[JsValue]
        }
    }

    "use a generic https pool" in {
//      val pool = Http().superPool()
      pending
    }
  }
}
