package implementation.edition.server.curation

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Sink, Source}
import domain.edition.Models
import Models.{EditionInfo, EditionNames, MtgEdition}
import domain.edition.server.BatchToServerSentEventSupport
import implementation.edition.mongo.MongoRepository._
import implementation.common.persistence.mongo.MongoPersistenceSink

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.implicitConversions

class EditionCurationAndPersistence(implicit actorSystem: ActorSystem, ec : ExecutionContextExecutor) extends BatchToServerSentEventSupport {
  implicit def sluggify(s: String): MagicCardSetSluggifier = new MagicCardSetSluggifier(s)

  def scrapAndSaveAndSendSSEEvents(): Source[ServerSentEvent, NotUsed] =
    Source.future(editionInfoListFuture)
      .mapConcat(identity)
      .alsoTo(new MongoPersistenceSink(editionCollection).sink)
      .map(_.toString)
      .map(ServerSentEvent(_))

  def batchStartRoute : Route =
    pathPrefix("edition") {
      path("start") {
        get {
          complete {
            scrapAndSaveAndSendSSEEvents()
          }
        }
      }
    }

  def editionInfoListFuture: Future[List[EditionInfo]] = {
    val gathererDataFuture: Future[List[Models.GathererNameToCode]] = Source.fromPublisher(gathererCodeCollection.find()).runWith(Sink.seq).map(_.toList)
    val dateDataFuture: Future[List[MtgEdition]] = Source.fromPublisher(mtgEditionCollection.find()).runWith(Sink.seq).map(_.toList)
    val languageDataFuture: Future[List[EditionNames]] = Source.fromPublisher(editionNamesCollection.find()).runWith(Sink.seq).map(_.toList)

    for {
      gathererData <- gathererDataFuture
      dateData <- dateDataFuture
      languageData <- languageDataFuture
    } yield {
      val dateDataCodeLookup = dateData.flatMap(set => (set.code -> set) :: set.secondaryCode.map(_ -> set).toList).toMap
      val dateDataNameLookup = dateData.map(set => set.name -> set).toMap
      val gathererWithDate: List[(Models.GathererNameToCode, Models.MtgEdition)] = gathererData
        .map(gatherer => gatherer -> dateDataCodeLookup.get(cleanGathererCode(gatherer.code)).orElse(dateDataNameLookup.get(cleanGathererName(gatherer.name))))
        .collect { case (gatherer, Some(mtgEdition)) => (gatherer, mtgEdition) }
      val languageLookup = languageData.map(language => language.englishName.slug() -> language).toMap
      gathererWithDate
        .map { case (gatherer, date) => (gatherer, date, languageLookup.get(gatherer.name.slug()).orElse(languageLookup.get(date.name.slug()))) }
        .map { case (gatherer, date, foreign) =>
          EditionInfo(
            gathererName = gatherer.name,
            languageToNamesMap = foreign.map(_.names).getOrElse(Map.empty),
            codes = List(Option(gatherer.code), Option(date.code), date.secondaryCode).flatten.distinct,
            yearMonth = date.yearMonth.get,
            releaseDate = date.releaseDate.get
          )
        }
    }
  }

  def cleanGathererCode(gathererCode : String) : String =
    gathererCode
      .split("_")
      .filter(_ != "MPS")
      .head

  def cleanGathererName(gathererName : String) : String =
    gathererName
      .replace("Box Set", "")
      .replace("\"Timeshifted\"", "")
      .replace("Gift Pack", "Core Set 2019")
      .trim
}

