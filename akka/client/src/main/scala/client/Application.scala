package client // Only bothering with a package for this example because Jackson breaks on types not in a package

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.lambdaworks.jacks.JacksMapper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Client app that uses Akka HTTP to call an ASP.NET Web API service.
 * @note This could all be a lot cleaner, but for the first attempt I'd like to see all the logic in one place so I can fit it all in my head :)
 */
object Application extends App {
  // IDEA reckons this import isn't required, but Jackson breaks at runtime without it.
  // I don't really understand IDEA, Scala, OR Jackson well enough yet to understand why this is the case, but I'm keen to figure it out.
  import TypeManifests._
  
  // Implicits required by Akka Streams / Akka HTTP
  implicit val clientSystem = ActorSystem("HttpClient")
  implicit val clientFlowMaterializer = ActorMaterializer()
  implicit val clientFlowExecutionContext: ExecutionContext = clientSystem.dispatcher

  // A one-off HTTP request.
  Http().singleRequest(
    HttpRequest(uri = "http://localhost:19123/api/v1/greet/me?name=Akka")
  ) andThen {
    // How'd we go?
    case Success(response) =>
      if (response.status.isSuccess()) {
        println("Response's HTTP status code indicates success.")

        println("Initiating stream read / parse...")
        deserializeBody(response) andThen {
          case Success(greeting) => println("Deserialised body: " + greeting)
          case Failure(error) =>    println(s"Read / deserialise failed ($error)")
        } andThen {
          case _ =>
            println("System shutdown because it's time...")
            shutdown()
        }
        println("Stream read / parse initiated.")
      }
      else {
        println("Response's HTTP status code indicates failure:" + response.status)
        response.entity.contentLengthOption match {
          case Some(contentLength) =>
            if (contentLength > 0) {
              println()
              println(
                Unmarshal(response.entity).to[String]
              )
            }
          case None =>
        }
      }
  } recover {
    case error =>
      println(s"HTTP request failed ($error)")

      println("System shutdown due to failure...")
      shutdown()
  }

  clientSystem.awaitTermination()
  println("System shutdown complete.")

  /**
   * Deserialise the body of the supplied `HttpResponse` as JSON.
   * @param response The `HttpResponse`.
   * @param materializer The `ActorMaterializer` used to build the Akka Streams data flow that processes the body content.
   * @param bodyTypeManifest A manifest reference required by Jackson so it knows what `TBody` is.
   * @tparam TBody The type into which the body should be deserialised.
   * @return A `Future[TBody]` representing asynchronous deserialisation of the response body.
   * @note Should probably be using the Unmarshal stuff here; refactor once we figure out all the inter-related bits and pieces required to do so.
   */
  def deserializeBody[TBody](response: HttpResponse)(implicit materializer: ActorMaterializer, bodyTypeManifest: Manifest[TBody]): Future[TBody] = {
    response.entity.dataBytes.runWith(Sink.head)(materializer).map { bytes =>
      println("Reading...")
      val responseBytes = bytes.toByteBuffer.array

      println("Parsing...")
      JacksMapper.readValue[TBody](responseBytes)
    }
  }

  /**
   * Cleanly shut down the HTTP connection pool and actor system.
   * @return A `Future[Unit]` representing the asynchronous shutdown process.
   * @note This only initiates the shutdown process (you'll still need to call actorSystem.awaitTermination).
   */
  def shutdown()(implicit actorSystem: ActorSystem): Future[Unit] = {
    Http(actorSystem).shutdownAllConnectionPools() andThen {
      case Failure(error) =>
        println("An error occurred while shutting down HTTP connection pools:")
        println(error)
      case _ => println("All HTTP connection pools have been closed.")
    } andThen {
      case _ => actorSystem.shutdown()
    }
  }

  /**
   * Represents a greeting from ASP.NET web API.
   * @param name The name of the caller.
   * @param greeting A caller-specific greeting.
   */
  case class Greeting(name: String, greeting: String)

  /**
   * Implicits for the Scala `Manifest`s representing well-known types (required by Jackson serialisation).
   */
  object TypeManifests {
    /**
     * Manifest for `Greeting`.
     * @note I suspect I may be missing a cleaner way to configure Jackson when writing generic types.
     *       Maybe consider Spray's JSON support, too (although it's uglier).
     */
    implicit val greetingManifest: Manifest[Greeting] = Manifest.classType(classOf[Greeting])
  }
}
