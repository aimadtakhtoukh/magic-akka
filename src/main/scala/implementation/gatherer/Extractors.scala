package implementation.gatherer

import akka.actor.Scheduler
import domain.gatherer.Models._
import domain.gatherer.midToDocuments.{MidToDocuments, SetCode}
import implementation.gatherer.Documents.{getDocument, printedCardUrl}
import implementation.gatherer.GathererScrapper.getMultiverseIdFromUrl
import org.jsoup.nodes.{Element, TextNode}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

object Extractors {

  private val colorSet = Set("W", "U", "B", "R", "G")

  def toModel(colorLetter: String) : Color = colorLetter match {
    case "W" => White
    case "U" => Blue
    case "B" => Black
    case "R" => Red
    case "G" => Green
  }

  trait Extractor[T] {
    def extract(implicit mid: MidToDocuments): T
  }

  trait ValueFromSelector {
    def getValueFromSelector(selector: String)(implicit midToDoc: MidToDocuments): String =
      midToDoc.cardComponent.select(selector).text
  }

  trait ColorFromText {
    def getColors(text: String): Set[String] =
      "\\{(.*?)}".r
        .findAllMatchIn(text)
        .map(_ group 1)
        .flatMap(_.toList.map(_.toString))
        .toSet.intersect(colorSet)
  }

  trait CreatureStatsValue {
    def extractCreatureValue(mid: MidToDocuments)(side: Int): Option[String] =
      Option(mid.cardComponent)
        .map(_.select("[id$=ptRow]"))
        .filter(_.select(".label").text() equals "P/T:")
        .map(_.select(".value").text)
        .map(_.split("/")(side).trim)
  }

  trait ManaImageToString {
    def manaImageToString(url: String): Option[String] = {
      val manaCostRegex = "^.*?name=(.*?)&.*?$".r
      Option(url)
        .map({ case manaCostRegex(manaCost) => manaCost })
        .map(c => s"{$c}")
    }
  }

  //TODO : labels, links to other cards

  object IdExtractor extends Extractor[String] {

    import CardNumberInSetExtractor.{extract => cardNumberInSet}
    import EditionCodeExtractor.{extract => editionCode}

    override def extract(implicit mid: MidToDocuments): String = f"$editionCode%s_$cardNumberInSet%4s".replace(" ", "0")
  }

  object EditionCodeExtractor extends Extractor[SetCode] {
    override def extract(implicit mid: MidToDocuments): SetCode = mid.setCode
  }

  object ManaCostExtractor extends Extractor[Option[String]] with ManaImageToString {
    override def extract(implicit mid: MidToDocuments): Option[String] =
      mid.cardComponent.select("[id$=manaRow] .value img").asScala
        .map(_.attr("src"))
        .flatMap(manaImageToString)
      match {
        case c if c.isEmpty => None
        case c => Some(c.mkString)
      }
  }

  object ColorFromManaCostExtractor extends Extractor[Set[Color]] with ColorFromText {

    import ManaCostExtractor.{extract => manaCost}

    override def extract(implicit mid: MidToDocuments): Set[Color] =
      manaCost(mid)
        .map(getColors)
        .getOrElse(Set.empty)
        .map(toModel)
  }

  object ColorFromTextExtractor extends Extractor[Set[Color]] {

    import CardTextExtractor.{extract => cardText}

    override def extract(implicit mid: MidToDocuments): Set[Color] =
      (cardText(mid).mkString match {
        case c if c contains "Devoid" => Set.empty
        case c if c contains "is all colors." => colorSet
        case _ => Set.empty
      }).map(toModel)
  }

  object ColorIndicatorExtractor extends Extractor[Set[Color]] with ValueFromSelector {
    override def extract(implicit mid: MidToDocuments): Set[Color] =
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
        .map(toModel)
  }

  object ColorExtractor extends Extractor[List[Color]] {

    import ColorFromManaCostExtractor.{extract => colorFromManaCost}
    import ColorFromTextExtractor.{extract => colorFromText}
    import ColorIndicatorExtractor.{extract => colorIndicator}

    override def extract(implicit mid: MidToDocuments): List[Color] =
      colorFromManaCost union colorFromText union colorIndicator toList
  }

  object CardTextExtractor extends Extractor[List[String]] with ManaImageToString {
    override def extract(implicit mid: MidToDocuments): List[String] = {
      val elements = mid.cardComponent.select("[id$=textRow] .value .cardtextbox")
      elements.select("img").forEach(
        e => e.replaceWith(new TextNode(manaImageToString(e.attr("src")).getOrElse("")))
      )
      elements.asScala.toList.map(_.text)
    }
  }

  object ColorIdentityExtractor extends Extractor[List[Color]] with ColorFromText {
    import CardTextExtractor.{extract => cardText}
    import ColorExtractor.{extract => colors}
    import ManaCostExtractor.{extract => manaCost}

    override def extract(implicit mid: MidToDocuments): List[Color] =
      (((colors(mid).toSet
        union getColors(cardText(mid).mkString).map(toModel))
        union getColors(manaCost(mid).mkString).map(toModel))
        toList)
  }

  object NameExtractor extends Extractor[String] with ValueFromSelector {
    override def extract(implicit mid: MidToDocuments): String = getValueFromSelector("[id$=nameRow] .value")
  }

  object ConvertedManaCostExtractor extends Extractor[Option[String]] with ValueFromSelector {
    override def extract(implicit mid: MidToDocuments): Option[String] =
      Option(getValueFromSelector("[id$=cmcRow] .value")).filterNot(_.isEmpty)
  }

  object TypesExtractor extends Extractor[String] with ValueFromSelector {
    override def extract(implicit mid: MidToDocuments): String = getValueFromSelector("[id$=typeRow] .value")
  }

  object SuperTypesExtractor extends Extractor[List[String]] {

    import TypesExtractor.{extract => types}

    override def extract(implicit mid: MidToDocuments): List[String] =
      types.split("—")(0).split(" ").toList.filterNot(_.isEmpty)
  }

  object SubTypesExtractor extends Extractor[List[String]] {

    import TypesExtractor.{extract => types}

    override def extract(implicit mid: MidToDocuments): List[String] =
      types(mid).split("—") match {
        case array if array.size < 2 => Nil
        case array => array(1).split(" ").toList.filterNot(_.isEmpty)
      }
  }

  object FlavorTextExtractor extends Extractor[Option[String]] {
    override def extract(implicit mid: MidToDocuments): Option[String] =
      Option(mid)
        .map(_.cardComponent)
        .map(_.select("[id$=FlavorText]").text())
        .filterNot(_.isEmpty)
  }

  object PowerExtractor extends Extractor[Option[String]] with CreatureStatsValue {
    override def extract(implicit mid: MidToDocuments): Option[String] = extractCreatureValue(mid)(0)
  }

  object ToughnessExtractor extends Extractor[Option[String]] with CreatureStatsValue {
    override def extract(implicit mid: MidToDocuments): Option[String] = extractCreatureValue(mid)(1)
  }

  object LoyaltyExtractor extends Extractor[Option[String]] {
    override def extract(implicit mid: MidToDocuments): Option[String] =
      Option(mid.cardComponent)
        .map(_.select("[id$=ptRow]"))
        .filter(_.select(".label").text() equals "Loyalty:")
        .map(_.select(".value").text)
  }

  object EditionExtractor extends Extractor[String] with ValueFromSelector {
    override def extract(implicit mid: MidToDocuments): String = getValueFromSelector("[id$=setRow] .value")
  }

  object RarityExtractor extends Extractor[String] with ValueFromSelector {
    override def extract(implicit mid: MidToDocuments): String = getValueFromSelector("[id$=rarityRow] .value")
  }

  object CardNumberInSetExtractor extends Extractor[String] with ValueFromSelector {
    override def extract(implicit mid: MidToDocuments): String = getValueFromSelector("[id$=numberRow] .value")
  }

  object ArtistExtractor extends Extractor[String] with ValueFromSelector {
    override def extract(implicit mid: MidToDocuments): String = getValueFromSelector("[id$=artistRow] .value")
  }

  object RulingsExtractor extends Extractor[List[Ruling]] {
    override def extract(implicit mid: MidToDocuments): List[Ruling] =
      mid.cardComponent
        .select("[id$=rulingsContainer] tbody tr")
        .asScala
        .map(tr => Ruling(
          date = tr.select("td[id$=rulingDate]").text,
          rule = tr.select("td[id$=rulingText]").text)
        )
        .toList
  }

  object LegalitiesExtractor extends Extractor[List[LegalityInFormat]] {
    def decodeLegality(field : String) : Legality = field match {
      case "Legal" => Legal
      case "Banned" => Banned
      case "Restricted" => Restricted
    }

    override def extract(implicit mid: MidToDocuments): List[LegalityInFormat] =
      mid.legalityDocument
        .select("table.cardList")
        .asScala
        .filter(_.select("tr.headerRow").text().contains("Legality"))
        .flatMap(_.select("tr.cardItem").asScala)
        .map(e =>
          LegalityInFormat(
            e.select("td.column1").text(),
            decodeLegality(e.select("td[style]").text())
          )
        )
        .filterNot(_.format.isEmpty)
        .toList
  }

  object LanguagesExtractor {

    import CardNumberInSetExtractor.{extract => cardNumberInSet}
    import CardTextExtractor.{extract => cardText}
    import FlavorTextExtractor.{extract => flavorText}
    import NameExtractor.{extract => name}
    import SubTypesExtractor.{extract => subTypes}
    import SuperTypesExtractor.{extract => superTypes}

    def extract(implicit midToDoc: MidToDocuments, ec: ExecutionContext, s: Scheduler): Future[List[Language]] = {
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

    private def multiverseIdFromElement(element: Element): String =
      Option(element.selectFirst("img[id$=cardImage]"))
        .map(_.attr("src"))
        .map(_.split("[&?]"))
        .map(_.filter(_ startsWith "multiverseid="))
        .map(_.map(_.replace("multiverseid=", "")))
        .flatMap(_.headOption)
        .getOrElse("")
  }

}
