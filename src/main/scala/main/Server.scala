package main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import edition.domain.server.BatchToServerSentEventSupport
import edition.implementation.server.curation.EditionCurationAndPersistence
import edition.implementation.server.scrapperToPersistence._

import scala.concurrent.ExecutionContextExecutor

object Server extends App {
  implicit val system: ActorSystem = ActorSystem("Scrapping_Jobs")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val magicCardSetScrapAndSave = new MagicCardSetArchiveScrapperToPersistence()
  val gathererScrapAndSave = new GathererScrapperToPersistence()
  val dateScrapAndSave = new DateScrapperToPersistence()
  val editionCurationAndPersistence = new EditionCurationAndPersistence()

  val sseBatches : List[BatchToServerSentEventSupport] =
    List(magicCardSetScrapAndSave, gathererScrapAndSave, dateScrapAndSave, editionCurationAndPersistence)

  val sseBatchesRoute = pathPrefix("batch") {
    sseBatches.map(_.batchStartRoute).reduce(_ ~ _)
  }

  val serverRoute = sseBatchesRoute

  Http().bindAndHandle(serverRoute, "localhost", 8080)
  println(s"Server online at http://localhost:8080/")
}
