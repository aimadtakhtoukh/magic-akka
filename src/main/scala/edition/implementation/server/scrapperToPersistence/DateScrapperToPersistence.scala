package edition.implementation.server.scrapperToPersistence

import akka.actor.ActorSystem
import common.implementation.persistence.mongo.MongoPersistenceSink
import edition.domain.Models.MtgEdition
import edition.implementation.persistance.mongo.MongoRepository._
import edition.implementation.scrappers.date.MagicCardSetDateScrapperSource

import scala.concurrent.ExecutionContextExecutor

class DateScrapperToPersistence(implicit actorSystem: ActorSystem, ec : ExecutionContextExecutor)
  extends ScrapperToPersistence[MtgEdition](
    "mtgEdition",
    new MagicCardSetDateScrapperSource,
    new MongoPersistenceSink[MtgEdition](mtgEditionCollection)
  )
