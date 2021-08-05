package implementation.edition.server.scrapperToPersistence

import akka.actor.ActorSystem
import domain.edition.Models.MtgEdition
import implementation.edition.mongo.MongoRepository._
import implementation.edition.scrappers.date.MagicCardSetDateScrapperSource
import implementation.common.persistence.mongo.MongoPersistenceSink

import scala.concurrent.ExecutionContextExecutor

class DateScrapperToPersistence(implicit actorSystem: ActorSystem, ec : ExecutionContextExecutor)
  extends ScrapperToPersistence[MtgEdition](
    "mtgEdition",
    new MagicCardSetDateScrapperSource,
    new MongoPersistenceSink[MtgEdition](mtgEditionCollection)
  )
