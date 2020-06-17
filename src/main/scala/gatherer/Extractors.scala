package gatherer

import org.jsoup.nodes.{Element, TextNode}
import GathererScrapper.getMultiverseIdFromUrl
import Documents.{getDocument, printedCardUrl}

import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.concurrent.{ExecutionContext, Future}

object Extractors {

  type Extractor[T] = MidToDocuments => T

  private val colorSet = Set("W", "U", "B", "R", "G")

  private def getValueFromSelector(selector : String)(implicit midToDoc : MidToDocuments): String =
    midToDoc.cardComponent.select(selector).text

  //TODO : labels, links to other cards

  def id(implicit midToDocuments: MidToDocuments) : String =
    s"${editionCode}_$cardNumberInSet"

  def colors(implicit midToDocuments: MidToDocuments) : List[String] =
    colorFromManaCost union colorFromText union colorIndicator toList

  def colorFromManaCost(implicit midToDocuments: MidToDocuments) : Set[String] =
    manaCost.map(getColors).getOrElse(Set.empty)

  def colorFromText(implicit midToDocuments: MidToDocuments) : Set[String] =
    cardText.mkString match {
      case c if c contains "Devoid" => Set.empty
      case c if c contains "is all colors." => colorSet
      case _ => Set.empty
    }

  def colorIndicator(implicit midToDocuments: MidToDocuments) : Set[String] =
    getValueFromSelector("[id$=colorIndicatorRow] .value")
      .split(",")
      .map(_.trim)
      .collect {
        case "White" => "W"
        case "Blue" => "U"
        case "Black" => "B"
        case "Red" => "R"
        case "Green" => "G"
      }
      .toSet

  def colorIdentity(implicit midToDocuments: MidToDocuments) : List[String] =
    colors.toSet union getColors(cardText.mkString) union getColors(manaCost.mkString) toList

  private def getColors(text: String) : Set[String] =
    "\\{(.*?)\\}".r
      .findAllMatchIn(text)
      .map(_ group 1)
      .flatMap(_.toList.map(_.toString))
      .toSet.intersect(colorSet)

  def editionCode(implicit midToDocuments: MidToDocuments) : String =
    midToDocuments.setCode

  def name(implicit midToDocuments : MidToDocuments) : String =
    getValueFromSelector("[id$=nameRow] .value")

  def manaCost(implicit midToDoc : MidToDocuments) : Option[String] =
    midToDoc.cardComponent.select("[id$=manaRow] .value img").asScala
      .map(_.attr("src"))
      .flatMap(manaImageToString)
    match {
      case c if c.isEmpty => None
      case c => Some(c.mkString)
    }

  private def manaImageToString(url : String) : Option[String] = {
    val manaCostRegex = "^.*?name=(.*?)&.*?$".r
    Option(url)
      .map({ case manaCostRegex(manaCost) => manaCost})
      .map(c => s"{$c}")
  }

  def cmc(implicit midToDocs : MidToDocuments) : Option[String] =
    Option(getValueFromSelector("[id$=cmcRow] .value"))
      .filterNot(_.isEmpty)

  def types(implicit midToDocs : MidToDocuments) :  String =
    getValueFromSelector("[id$=typeRow] .value")

  def superTypes(implicit mid : MidToDocuments) : List[String] =
    types(mid).split("—")(0).split(" ").toList.filterNot(_.isEmpty)

  def subTypes(implicit mid : MidToDocuments) : List[String] =
    types(mid).split("—") match {
      case array if array.size < 2 => Nil
      case array => array(1).split(" ").toList.filterNot(_.isEmpty)
    }

  def cardText(implicit mid : MidToDocuments) : List[String] = {
    val elements = mid.cardComponent.select("[id$=textRow] .value .cardtextbox")
    elements.select("img").forEach(
      e => e.replaceWith(new TextNode(manaImageToString(e.attr("src")).getOrElse("")))
    )
    elements.asScala.toList.map(_.text)
  }

  def flavorText(implicit mid : MidToDocuments) : Option[String] =
    Option(mid.cardComponent)
      .map(_.select("[id$=FlavorText]").text())
      .filterNot(_.isEmpty)

  def power(implicit mid : MidToDocuments) : Option[String] =
    Option(mid.cardComponent)
      .map(_.select("[id$=ptRow]"))
      .filter(_.select(".label").text() equals "P/T:")
      .map(_.select(".value").text)
      .map(_.split("/")(0).trim)

  def toughness(implicit mid : MidToDocuments) : Option[String] =
    Option(mid.cardComponent)
      .map(_.select("[id$=ptRow]"))
      .filter(_.select(".label").text() equals "P/T:")
      .map(_.select(".value").text)
      .map(_.split("/")(1).trim)

  def loyalty(implicit mid : MidToDocuments) : Option[String] =
    Option(mid.cardComponent)
      .map(_.select("[id$=ptRow]"))
      .filter(_.select(".label").text() equals "Loyalty:")
      .map(_.select(".value").text)

  def expansion(implicit mid : MidToDocuments) : String =
    getValueFromSelector("[id$=setRow] .value")

  def rarity(implicit mid : MidToDocuments) : String =
    getValueFromSelector("[id$=rarityRow] .value")

  def cardNumberInSet(implicit mid : MidToDocuments) : String =
    getValueFromSelector("[id$=numberRow] .value")

  def artist(implicit mid : MidToDocuments) : String =
    getValueFromSelector("[id$=artistRow] .value")

  def rulings(implicit mid : MidToDocuments) : List[Ruling] =
    mid.cardComponent
      .select("[id$=rulingsContainer] tbody tr")
      .asScala
      .map(tr => Ruling(
        date = tr.select("td[id$=rulingDate]").text,
        rule = tr.select("td[id$=rulingText]").text)
      )
      .toList


  def legalities(implicit mid : MidToDocuments) : List[Legality] =
    mid.legalityDocument
      .select("table.cardList")
      .asScala
      .filter(_.select("tr.headerRow").text().contains("Legality"))
      .flatMap(_.select("tr.cardItem").asScala)
      .map(e =>
        Legality(
          e.select("td.column1").text(),
          e.select("td[style]").text()
        )
      )
      .filterNot(e => e.format.isEmpty && e.legality.isEmpty)
      .toList

  def languages(midToDoc : MidToDocuments)(implicit ec : ExecutionContext) : Future[List[Language]] = {
      val numberInSet = cardNumberInSet(midToDoc)
      Future.sequence({
        val languages = midToDoc.languageDocuments.flatMap(_.select("table.cardList tr.cardItem").asScala)
          .map(tr => (
            getMultiverseIdFromUrl(tr.select("td:nth-child(1) a").attr("href")),
            tr.select("td:nth-child(2)").text())
          )
        (languages ::: (Some(midToDoc.mid), "English") :: Nil)
          .map {
            case (multiverseId, language) =>
              multiverseId
                .map(printedCardUrl)
                .map(getDocument)
                .map(
                  _.map(details => MidToDocuments(
                    mid = multiverseId.get,
                    setCode = midToDoc.setCode,
                    detailsDocument = details,
                    cardComponent =
                      details.select(".cardComponentContainer")
                        .asScala
                        .filterNot(_.text().isEmpty)
                        .find(multiverseIdFromElement(_) equals multiverseId.get)
                        .getOrElse(details.select(".cardComponentContainer").get(1)),
                    languageDocuments = Nil,
                    legalityDocument = null
                  ))
                    .map(
                      midToDoc => Language(
                        multiverseId = multiverseId.get,
                        name = name(midToDoc),
                        types = superTypes(midToDoc) ::: subTypes(midToDoc),
                        cardText = cardText(midToDoc),
                        flavorText = flavorText(midToDoc),
                        numberInSet = cardNumberInSet(midToDoc),
                        language = language
                      )
                    )
                )
                .getOrElse(Future.failed(new IllegalArgumentException("Document non trouvé")))
          }
      })
        .map(_.filter(_.numberInSet equals numberInSet))
    }

    def multiverseIdFromElement(element : Element) : String =
      Option(element.selectFirst("img[id$=cardImage]"))
        .map(_.attr("src"))
        .map(_.split("[&?]"))
        .map(_.filter(_ startsWith "multiverseid="))
        .map(_.map(_.replace("multiverseid=", "")))
        .flatMap(_.headOption)
        .getOrElse("")
}
