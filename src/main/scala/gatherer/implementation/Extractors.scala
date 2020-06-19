package gatherer.implementation

import gatherer.GathererScrapper.getMultiverseIdFromUrl
import org.jsoup.nodes.{Element, TextNode}
import gatherer.implementation.Documents.{getDocument, printedCardUrl}
import gatherer.domain.midToDocuments.{MidToDocuments, SetCode}
import gatherer.domain.Models.{Language, Legality, LegalityInFormat, Ruling}

import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.concurrent.{ExecutionContext, Future}


object Extractors {

  private val colorSet = Set("W", "U", "B", "R", "G")

  trait Extractor[T] {
    def extract(implicit mid : MidToDocuments): T
  }

  trait ValueFromSelector {
    def getValueFromSelector(selector : String)(implicit midToDoc : MidToDocuments): String =
      midToDoc.cardComponent.select(selector).text
  }

  trait ColorFromText {
    def getColors(text: String) : Set[String] =
      "\\{(.*?)}".r
        .findAllMatchIn(text)
        .map(_ group 1)
        .flatMap(_.toList.map(_.toString))
        .toSet.intersect(colorSet)
  }

  trait CreatureStatsValue {
    def extractCreatureValue(mid : MidToDocuments)(side : Int): Option[String] =
      Option(mid.cardComponent)
        .map(_.select("[id$=ptRow]"))
        .filter(_.select(".label").text() equals "P/T:")
        .map(_.select(".value").text)
        .map(_.split("/")(side).trim)
  }

  trait ManaImageToString {
    def manaImageToString(url : String) : Option[String] = {
      val manaCostRegex = "^.*?name=(.*?)&.*?$".r
      Option(url)
        .map({ case manaCostRegex(manaCost) => manaCost})
        .map(c => s"{$c}")
    }
  }

  //TODO : labels, links to other cards

  object IdExtractor extends Extractor[String] {
    import EditionCodeExtractor.{extract => editionCode}
    import CardNumberInSetExtractor.{extract => cardNumberInSet}
    override def extract(implicit mid : MidToDocuments): String = s"${editionCode}_$cardNumberInSet"
  }

  object EditionCodeExtractor extends Extractor[SetCode] {
    override def extract(implicit mid : MidToDocuments): SetCode = mid.setCode
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

  object ColorFromManaCostExtractor extends Extractor[Set[String]] with ColorFromText {
    import ManaCostExtractor.{extract => manaCost}
    override def extract(implicit mid: MidToDocuments): Set[String] = manaCost(mid).map(getColors).getOrElse(Set.empty)
  }

  object ColorFromTextExtractor extends Extractor[Set[String]] {
    import CardTextExtractor.{extract => cardText}
    override def extract(implicit mid: MidToDocuments): Set[String] =
      cardText(mid).mkString match {
        case c if c contains "Devoid" => Set.empty
        case c if c contains "is all colors." => colorSet
        case _ => Set.empty
      }
  }

  object ColorIndicatorExtractor extends Extractor[Set[String]] with ValueFromSelector {
    override def extract(implicit mid: MidToDocuments): Set[String] =
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
  }

  object ColorExtractor extends Extractor[List[String]] {
    import ColorFromManaCostExtractor.{extract => colorFromManaCost}
    import ColorFromTextExtractor.{extract => colorFromText}
    import ColorIndicatorExtractor.{extract => colorIndicator}

    override def extract(implicit mid: MidToDocuments): List[String] =
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

  object ColorIdentityExtractor extends Extractor[List[String]] with ColorFromText {
    import ColorExtractor.{extract => colors}
    import CardTextExtractor.{extract => cardText}
    import ManaCostExtractor.{extract => manaCost}

    override def extract(implicit mid: MidToDocuments): List[String] =
      colors(mid).toSet union getColors(cardText(mid).mkString) union getColors(manaCost(mid).mkString) toList
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
    override def extract(implicit mid: MidToDocuments): List[LegalityInFormat] =
      mid.legalityDocument
        .select("table.cardList")
        .asScala
        .filter(_.select("tr.headerRow").text().contains("Legality"))
        .flatMap(_.select("tr.cardItem").asScala)
        .map(e =>
          LegalityInFormat(
            e.select("td.column1").text(),
            Legality.withName(e.select("td[style]").text())
          )
        )
        .filterNot(_.format.isEmpty)
        .toList
  }

  object LanguagesExtractor {
    import CardNumberInSetExtractor.{extract => cardNumberInSet}
    import NameExtractor.{extract => name}
    import SuperTypesExtractor.{extract => superTypes}
    import SubTypesExtractor.{extract => subTypes}
    import CardTextExtractor.{extract => cardText}
    import FlavorTextExtractor.{extract => flavorText}

    def extract(implicit midToDoc : MidToDocuments, ec : ExecutionContext) : Future[List[Language]] = {
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

    private def multiverseIdFromElement(element : Element) : String =
      Option(element.selectFirst("img[id$=cardImage]"))
        .map(_.attr("src"))
        .map(_.split("[&?]"))
        .map(_.filter(_ startsWith "multiverseid="))
        .map(_.map(_.replace("multiverseid=", "")))
        .flatMap(_.headOption)
        .getOrElse("")
  }

}
