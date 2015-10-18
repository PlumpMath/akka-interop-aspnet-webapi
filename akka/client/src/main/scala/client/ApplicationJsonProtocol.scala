package client

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import models.Greeting
import spray.json.DefaultJsonProtocol

/**
 * Custom JSON protocol for well-known data contracts used by this example.
 */
object ApplicationJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  /**
   * Custom JSON format for `Greeting`.
   */
  implicit val greetingFormat = jsonFormat2(Greeting)
}
