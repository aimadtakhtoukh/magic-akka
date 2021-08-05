package main

import akka.actor.{ActorSystem, Scheduler}
import domain.edition.Models.EditionNames
import implementation.common.persistence.mongo.MongoPersistenceSink
import implementation.edition.mongo.MongoRepository.editionNamesCollection
import implementation.edition.scrappers.languages.MagicCardSetArchiveScrapperSource

import scala.concurrent.ExecutionContextExecutor

object EditionNames extends App {
  implicit val system: ActorSystem = ActorSystem("Scrapping_Jobs")
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val scheduler: Scheduler = system.scheduler

  new MagicCardSetArchiveScrapperSource().source
    .runForeach(println)
    .onComplete(_ => system.terminate())

//  new MagicCardSetArchiveScrapperSource().source
//    .runWith(new MongoPersistenceSink[EditionNames](editionNamesCollection).sink)
//    .onComplete(_ => system.terminate())
}
