package implementation.gatherer

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.scaladsl.Flow
import domain.gatherer.Models.Card
import domain.gatherer.midToDocuments.MidToDocuments
import implementation.gatherer.Documents.{gathererPageUrl, gathererUrl, getDocument, languageUrl}
import org.jsoup.nodes.Document

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex
import scala.jdk.CollectionConverters._

object GathererScrapper {
  def flow()(implicit ec: ExecutionContext, s: Scheduler): Flow[String, Card, NotUsed] =
    Flow[String]
      .via(editionToMultiverseId)
      .mapAsyncUnordered(4) { case (cardName, mid, setCode) => MidToDocuments(cardName, mid, setCode) }
      .mapConcat(identity)
      .mapAsync(4)(extractCard)

  def getMultiverseIdFromUrl(url: String): Option[String] = url match {
    case multiverseRegex(multiverseId) => Some(multiverseId)
    case _ => None
  }

  def getLanguageDocuments(mid: String)(implicit ec: ExecutionContext, s: Scheduler): Future[List[Document]] =
    getDocument(languageUrl(mid))
      .flatMap(
        doc => doc.select("#ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_languageList_pagingControls").asScala match {
          case list if list.isEmpty => Future.successful(List(doc))
          case list => Future.sequence(list.flatMap(_.select("a").asScala).map(_ attr "href").distinct.toList.map(gathererUrl + _).map(getDocument))
        }
      )

  private def editionToMultiverseId(implicit ec: ExecutionContext, s: Scheduler): Flow[String, (String, String, String), NotUsed] =
    Flow[String]
      .map(getFullSetUrl)
      .mapAsyncUnordered(4)(getDocument)
      .mapConcat(_.select("table tr.cardItem").asScala.toList)
      .map(line => (line.select("td.name").text, line.select("td.printings a"), line.selectFirst("td.printings a img").attr("alt")))
      .mapConcat {
        case (cardName, prints, setCode) =>
          prints
            .asScala
            .map(_ attr "href")
            .map(getMultiverseIdFromUrl)
            .collect { case Some(multiverseId) => (cardName, multiverseId, setCode) }
            .toSet
      }

  private def extractCard(implicit ec: ExecutionContext, s: Scheduler): MidToDocuments => Future[Card] =
    implicit midToDocs => {
      import implementation.gatherer.Extractors._
      for {
        languages <- LanguagesExtractor.extract
      } yield {
        Card(
          mid = midToDocs.mid,
          id = IdExtractor.extract,
          name = NameExtractor.extract,
          manaCost = ManaCostExtractor.extract,
          cmc = ConvertedManaCostExtractor.extract,
          colors = ColorExtractor.extract,
          colorIdentity = ColorIdentityExtractor.extract,
          types = TypesExtractor.extract,
          superTypes = SuperTypesExtractor.extract,
          subTypes = SubTypesExtractor.extract,
          cardText = CardTextExtractor.extract,
          flavorText = FlavorTextExtractor.extract,
          power = PowerExtractor.extract,
          toughness = ToughnessExtractor.extract,
          loyalty = LoyaltyExtractor.extract,
          edition = EditionExtractor.extract,
          rarity = RarityExtractor.extract,
          cardNumberInSet = CardNumberInSetExtractor.extract,
          artist = ArtistExtractor.extract,
          editionCode = EditionCodeExtractor.extract,
          rulings = RulingsExtractor.extract,
          legalities = LegalitiesExtractor.extract,
          languages = languages
        )
      }
    }

  def getFullSetUrl(setName: String): String =
    s"$gathererPageUrl/Search/Default.aspx?output=compact&set=[%22${setName.replaceAll(" ", "+")}%22]&special=true"

  private val multiverseRegex: Regex = "^.*?multiverseid=(\\d+).*?$".r
}
