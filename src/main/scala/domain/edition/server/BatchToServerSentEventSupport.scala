package domain.edition.server

import akka.NotUsed
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source

//Abstraction to start batches through HTTP calls, answering via Server Sent Events
trait BatchToServerSentEventSupport {

  //Scrap, save, send events.
  def scrapAndSaveAndSendSSEEvents(): Source[ServerSentEvent, NotUsed]

  //Call the route to start the batch
  def batchStartRoute : Route

}
