package implementation.edition.server.scrapperToPersistence

import akka.actor.ActorSystem
import domain.edition.Models.EditionNames
import implementation.edition.mongo.MongoRepository._
import implementation.edition.scrappers.languages.MagicCardSetArchiveScrapperSource
import implementation.common.persistence.mongo.MongoPersistenceSink

import scala.concurrent.ExecutionContextExecutor

class MagicCardSetArchiveScrapperToPersistence(implicit actorSystem: ActorSystem, ec : ExecutionContextExecutor)
  extends ScrapperToPersistence[EditionNames](
    "editionNames",
    new MagicCardSetArchiveScrapperSource,
    new MongoPersistenceSink[EditionNames](editionNamesCollection)
  )
