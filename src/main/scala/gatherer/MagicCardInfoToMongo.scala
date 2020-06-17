package gatherer

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContextExecutor

object MagicCardInfoToMongo extends App {
  implicit val system: ActorSystem = ActorSystem("Scrapping_Jobs")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  MagicSetSource.allMagicSetSource
    .via(GathererScrapper.flow())
    .runWith(new MongoPersistenceSink(MongoRepository.cardCollection).sink)
    .onComplete {
      _ => system.terminate()
    }

}
