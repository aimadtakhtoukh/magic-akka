package edition.implementation.server.scrapperToPersistence

import akka.actor.ActorSystem
import edition.domain.Models.EditionNames
import edition.implementation.persistance.mongo.MongoPersistenceSink
import edition.implementation.persistance.mongo.MongoRepository._
import edition.implementation.scrappers.languages.MagicCardSetArchiveScrapperSource

import scala.concurrent.ExecutionContextExecutor

class MagicCardSetArchiveScrapperToPersistence(implicit actorSystem: ActorSystem, ec : ExecutionContextExecutor)
  extends ScrapperToPersistence[EditionNames](
    "editionNames",
    new MagicCardSetArchiveScrapperSource,
    new MongoPersistenceSink[EditionNames](editionNamesCollection)
  )
