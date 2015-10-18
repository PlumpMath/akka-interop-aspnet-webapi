package client // Only bothering with a package for this example because Jackson breaks on types not in a package

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.{Unmarshaller, Unmarshal}
import akka.stream.ActorMaterializer

import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Client app that uses Akka HTTP to call an ASP.NET Web API service.
 * @note This could all be a lot cleaner, but for the first attempt I'd like to see all the logic in one place so I can fit it all in my head :)
 */
object Application extends App {
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
        import ApplicationJsonProtocol._

        Unmarshal(response.entity).to[Greeting] andThen {
          case Success(greeting) => println("Deserialised body: " + greeting)
          case Failure(error)    => println(s"Read / deserialise failed ($error)")
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
          case None => println("No further information available (response's body was empty).")
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
   * Cleanly shut down the HTTP connection pool and actor system.
   * @return A `Future[Unit]` representing the asynchronous shutdown process.
   * @note This only initiates the shutdown process (you'll still need to call actorSystem.awaitTermination).
   */
  def shutdown()
    (implicit actorSystem: ActorSystem): Future[Unit] = {
    Http(actorSystem).shutdownAllConnectionPools() andThen {
      case Failure(error) =>
        println("An error occurred while shutting down HTTP connection pools:")
        println(error)
      case wel => println("All HTTP connection pools have been closed.")
    } andThen { case always =>
      actorSystem.shutdown()
    }
  }

  /**
   * Represents a greeting from ASP.NET web API.
   * @param name The name of the caller.
   * @param greeting A caller-specific greeting.
   */
  case class Greeting(name: String, greeting: String)

  /**
   * Custom JSON protocol for well-known data contracts used by this example.
   */
  private object ApplicationJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
    /**
     * Custom JSON format for `Greeting`.
     */
    implicit val greetingFormat = jsonFormat2(Greeting)

    /**
     * Custom unmarshaller for `Greeting`.
     * @note Seems like a major PITA to have to declare both the format and the unmarshaller for every body type.
     *       Surely there's a convenience shortcut somewhere?
     */
    implicit val greetingUnmarshaller: Unmarshaller[HttpEntity, Greeting] = sprayJsonUnmarshaller[Greeting]
  }
}