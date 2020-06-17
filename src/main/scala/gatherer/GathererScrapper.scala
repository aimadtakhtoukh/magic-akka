package gatherer

import akka.NotUsed
import akka.stream.scaladsl.Flow
import org.jsoup.nodes.Document
import Documents._

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object GathererScrapper {
  def flow()(implicit ec : ExecutionContext): Flow[String, Card, NotUsed] =
    Flow[String]
      .via(editionToMultiverseId)
      .mapAsyncUnordered(4) {case (cardName, mid, setCode) => MidToDocuments(cardName, mid, setCode)}
      .mapConcat(identity)
      .mapAsync(4)(extractCard)

  def getMultiverseIdFromUrl(url: String): Option[String] = url match {
    case multiverseRegex(multiverseId) => Some(multiverseId)
    case _ => None
  }

  def getLanguageDocuments(mid : String)(implicit ec : ExecutionContext) : Future[List[Document]] =
    getDocument(languageUrl(mid))
      .flatMap(
        doc => doc.select("#ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_languageList_pagingControls").asScala match {
          case list if list.isEmpty => Future.successful(List(doc))
          case list => Future.sequence(list.flatMap(_.select("a").asScala).map(_ attr "href").distinct.toList.map(gathererUrl + _).map(getDocument))
        }
      )

  private def editionToMultiverseId(implicit ec : ExecutionContext): Flow[String, (String, String, String), NotUsed] =
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
          .collect { case Some(multiverseId) => (cardName, multiverseId, setCode)}
          .toSet
    }

  private def extractCard(implicit ec : ExecutionContext) : MidToDocuments => Future[Card] =
    midToDocs => {
      import Extractors._
      for {
        languages <- languages(midToDocs)
      } yield {
        implicit val mid: MidToDocuments = midToDocs
        Card(
          mid = midToDocs.mid,
          id = id,
          name = name,
          manaCost = manaCost,
          cmc = cmc,
          colors = colors,
          colorIdentity = colorIdentity,
          types = types,
          superTypes = superTypes,
          subTypes = subTypes,
          cardText = cardText,
          flavorText = flavorText,
          power = power,
          toughness = toughness,
          loyalty = loyalty,
          expansion = expansion,
          rarity = rarity,
          cardNumberInSet = cardNumberInSet,
          artist = artist,
          editionCode = editionCode,
          rulings = rulings,
          legalities = legalities,
          languages = languages
        )
      }
    }

  def getFullSetUrl(setName : String) : String =
    s"$gathererPageUrl/Search/Default.aspx?output=compact&set=[%22${setName.replaceAll(" ", "+")}%22]&special=true"
  private val multiverseRegex: Regex = "^.*?multiverseid=(\\d+).*?$".r
}
