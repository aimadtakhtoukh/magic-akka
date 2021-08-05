package main

import akka.actor.{ActorSystem, Scheduler}
import akka.stream.scaladsl.{Flow, Sink, Source}
import implementation.common.persistence.mongo.MongoPersistenceSink
import implementation.gatherer.{GathererScrapper, MagicSetSource}
import implementation.gatherer.mongo.{MongoDTO, MongoRepository}
import org.json4s.{Formats, NoTypeHints}
import org.json4s.native.Serialization

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object MagicCardInfoToMongo extends App {
  implicit val formats: AnyRef with Formats = Serialization.formats(NoTypeHints)
  implicit val system: ActorSystem = ActorSystem("Scrapping_Jobs")
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val scheduler: Scheduler = system.scheduler

  //MagicSetSource.allMagicSetSource
  MagicSetSource.magicSetFromString("Core Set 2020")
//  MagicSetSource.magicSetFromString("Lorwyn")
//  MagicSetSource.magicSetFromString("Tenth Edition")
    .via(GathererScrapper.flow())
    .map(MongoDTO.Card.fromModel)
    .runWith(new MongoPersistenceSink(MongoRepository.cardCollection).sink)
    .recover {
      case e: Throwable => e.printStackTrace()
    }
    .onComplete {
      case Success(_) =>
        system.terminate()
      case Failure(exception) =>
        exception.printStackTrace()
        system.terminate()
    }

}
