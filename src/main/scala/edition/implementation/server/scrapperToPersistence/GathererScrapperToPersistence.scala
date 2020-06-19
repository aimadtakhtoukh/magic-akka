package edition.implementation.server.scrapperToPersistence

import akka.actor.ActorSystem
import common.implementation.persistence.mongo.MongoPersistenceSink
import edition.domain.Models.GathererNameToCode
import edition.implementation.persistance.mongo.MongoRepository._
import edition.implementation.scrappers.gatherer.MagicCardSetGathererScrapperSource

import scala.concurrent.ExecutionContextExecutor

class GathererScrapperToPersistence(implicit actorSystem: ActorSystem, ec : ExecutionContextExecutor)
  extends ScrapperToPersistence[GathererNameToCode](
    "gathererCode",
    new MagicCardSetGathererScrapperSource,
    new MongoPersistenceSink[GathererNameToCode](gathererCodeCollection)
  )