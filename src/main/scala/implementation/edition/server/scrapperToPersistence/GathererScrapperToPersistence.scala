package implementation.edition.server.scrapperToPersistence

import akka.actor.{ActorSystem, Scheduler}
import domain.edition.Models.GathererNameToCode
import implementation.edition.mongo.MongoRepository._
import implementation.edition.scrappers.gatherer.MagicCardSetGathererScrapperSource
import implementation.common.persistence.mongo.MongoPersistenceSink

import scala.concurrent.ExecutionContextExecutor

class GathererScrapperToPersistence(implicit actorSystem: ActorSystem, ec : ExecutionContextExecutor, s: Scheduler)
  extends ScrapperToPersistence[GathererNameToCode](
    "gathererCode",
    new MagicCardSetGathererScrapperSource,
    new MongoPersistenceSink[GathererNameToCode](gathererCodeCollection)
  )