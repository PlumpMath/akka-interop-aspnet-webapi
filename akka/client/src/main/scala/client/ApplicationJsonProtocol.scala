package client

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

/**
 * Custom JSON protocol for well-known data contracts used by this example.
 * @note The SprayJsonSupport trait pulls in some implicit converters required in order to generate a `Marshaller` / `Unmarshaller` for each `RootJsonFormat[_]`.
 */
object ApplicationJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  import models._

  /**
   * Custom JSON format for `Greeting`.
   */
  implicit val greetingFormat = jsonFormat2(Greeting)
}
