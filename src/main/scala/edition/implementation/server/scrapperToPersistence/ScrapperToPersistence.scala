package edition.implementation.server.scrapperToPersistence

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import common.domain.persistence.PersistenceSink
import edition.domain.scrappers.ScrapperSource
import edition.domain.server.BatchToServerSentEventSupport

import scala.concurrent.ExecutionContextExecutor

class ScrapperToPersistence[T](urlPath : String, scrapperSource: ScrapperSource[T], persistenceSink: PersistenceSink[T])
                              (implicit actorSystem: ActorSystem, ec : ExecutionContextExecutor) extends BatchToServerSentEventSupport {

  def scrapAndSaveAndSendSSEEvents(): Source[ServerSentEvent, NotUsed] =
    scrapperSource.source
    .alsoTo(persistenceSink.sink)
    .map(_.toString)
    .map(ServerSentEvent(_))

  def batchStartRoute : Route =
    pathPrefix(urlPath) {
      path("start") {
        get {
          complete {
            scrapAndSaveAndSendSSEEvents()
          }
        }
      }
    }
}
