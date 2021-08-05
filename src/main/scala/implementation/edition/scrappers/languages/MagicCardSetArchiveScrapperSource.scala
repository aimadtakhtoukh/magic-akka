package implementation.edition.scrappers.languages

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Concat, Flow, Source}
import org.jsoup.nodes.{Document, Element}
import implementation.edition.common.WebRequest
import domain.edition.Models.{Language, SetName, EditionNames}
import domain.edition.scrappers.ScrapperSource
import implementation.edition.scrappers.languages.extractor.{FeaturedInformationExtractor, InfoSetOnPageExtractor, TabNameExtractor, TitleExtractor}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

class MagicCardSetArchiveScrapperSource(implicit system: ActorSystem, executionContextExecutor: ExecutionContextExecutor)
  extends ScrapperSource[EditionNames] {

  sealed trait ExtractorType
  case object InfoSetOnPage extends ExtractorType
  case object FeaturedInformation extends ExtractorType
  case object TabName extends ExtractorType
  case object Title extends ExtractorType

  type ExtractorFunction = Document => Future[List[String]]

  implicit def webRequestFromUrl(url : String) : WebRequest = new WebRequest(url)

  private val productLinkRegex = "\".*?(/(?:node|products|content)/.*?)\"".r
  private val languagesCodes = List("en", "zh-hans", "zh-hant", "fr", "de", "it", "ja", "ko", "pt-br", "ru", "es")

  private val extractors : List[(ExtractorType, ExtractorFunction)] = List(
    (InfoSetOnPage, InfoSetOnPageExtractor.extract),
    (TabName, new TabNameExtractor().extract),
    (FeaturedInformation, FeaturedInformationExtractor.extract),
    (Title, TitleExtractor.extract)
  )

  def source : Source[EditionNames, NotUsed] =
    Source.combine(newCardSetProductElements, legacyCardSetArchiveElements)(Concat(_))
      .via(extractProductLinks)
      .mapAsync(1)(_.requestToDocument())
      .via(extractAltLanguageLinks)
      .map {_ map { case (lang, link) => (lang, link.requestToDocument(lang))}}
      .map {_ map { case (lang, doc) => doc.flatMap(extract).map(lang -> _)}}
      .mapAsync(1)(Future.sequence(_))
      .map {_ map { case (lang, names) => (lang, namesFromExtraction(names))}}
      .map {_ map { case (lang, names) => (lang, names.filterNot(_.isEmpty))} }
      .mapConcat (toEditionNames)

  private def extract(document: Document) : Future[List[(ExtractorType, List[String])]] =
    Future.sequence(
      extractors.map {
        case (extractorType, extractor) => extractor(document).map(extractorType -> _)
      }
    )

  private def legacyCardSetArchiveElements: Source[Element, NotUsed] =
    Source.future("https://magic.wizards.com/en/products/card-set-archive".requestToDocument())
      .map{_ selectFirst "#content"}
      .mapConcat(_.children().asScala.toList)

  private def newCardSetProductElements: Source[Element, NotUsed] =
    Source.future("https://magic.wizards.com/en/products".requestToDocument())
      .mapConcat(_.select("#content .gridder").asScala.toList)

  private def extractProductLinks : Flow[Element, String, NotUsed] =
    Flow[Element]
      .map(_.outerHtml())
      .mapConcat(productLinkRegex.findAllMatchIn(_).toList)
      .map(_ group 1)
      .map(link => s"https://magic.wizards.com$link")
      .statefulMapConcat { () =>
        var linkSet = Set.empty[String]

        { link : String =>
          if (linkSet(link)) {
            Nil // no element downstream if element is blacklisted
          } else {
            linkSet += link
            link :: Nil
          }
        }
      }

  private def extractAltLanguageLinks : Flow[Document, List[(String, String)], NotUsed] =
    Flow[Document]
      .map {
        document =>
          val altLinks = document.select("link[rel='alternate']").asScala.toList
            .map(elem => (elem attr "hreflang", s"https://magic.wizards.com${elem.attr("href")}"))
          altLinks match {
            case List() =>
              val canonicalLinkOption = document.select("link[rel='canonical']").asScala.headOption.map(_ attr "href")
              canonicalLinkOption
                .map {
                  canonicalLink =>
                    languagesCodes
                      .map { lang => (lang, canonicalLink.replaceAll("/en/", s"/$lang/"))}
                      .map {
                        case ("en", link) => ("en", link)
                        case (lang, link) => (lang, link.replaceAll("-bolas", ""))}
                }
                .getOrElse(Nil)
            case alt => alt
          }
      }

  private def namesFromExtraction(extractList : List[(ExtractorType, List[SetName])]) : List[SetName] = {
    val map = extractList.toMap
    val infoSetOnPage = map.get(InfoSetOnPage).filter(_.nonEmpty)
    val title = map.get(Title).filter(_.nonEmpty)
    val featuredInformation = map.get(FeaturedInformation).filter(_.nonEmpty)
    val tabName = map.get(TabName).filter(_.nonEmpty)
    infoSetOnPage.orElse(featuredInformation).orElse(tabName).orElse(title).getOrElse(List(""))
  }

  private def toEditionNames(extractInfo : List[(Language, List[SetName])]): List[EditionNames] =
    extractInfo
      .flatMap { case (language, setNameList) => setNameList.map(language -> _).zipWithIndex}
      .groupBy(_._2)
      .values
      .map(_.map({case (pair, _) => pair}).toMap)
      .map(_.map { case (lang, setName) => (
        lang,
        setName
          .replaceAll("\u00a0", " ")
          .replace("\\s+", " ")
          .trim)
      })
      .map { languageMap => (languageMap("en"), languageMap)}
      .map {
        case englishName -> languageMap =>
          englishName ->
            languageMap.map {
              case (lang, setName) if setName.contains(englishName) => (lang, englishName)
              case otherwise => otherwise
            }
      }
      .map { case englishName -> languageMap =>EditionNames(englishName, languageMap) }
      .toList
}