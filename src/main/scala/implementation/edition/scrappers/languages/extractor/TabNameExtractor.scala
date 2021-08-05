package implementation.edition.scrappers.languages.extractor

import org.jsoup.nodes.Document
import implementation.edition.common.WebRequest

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

class TabNameExtractor(implicit ec : ExecutionContext) extends Extractor {
  implicit def webRequestFromUrl(url : String) : WebRequest = new WebRequest(url)

  override def extract(doc: Document)(implicit ec: ExecutionContext): Future[List[String]] =
    Some(doc).flatMap(
      _.select("#block-system-main div.subsection-wrapper a.mn_subsection_game-info_products_magic-online_Info")
        .asScala
        .headOption
    ) match {
      case Some(el) => concatenatedPageUrl(el.attr("href")).requestToDocument().flatMap(FeaturedInformationExtractor.extract)
      case None => foreignNameFromSelector("#block-system-main div.subsection-wrapper a.current")(doc)
    }

  private def concatenatedPageUrl(pageUrl : String) = if (pageUrl startsWith "https://magic.wizards.com") pageUrl else s"https://magic.wizards.com$pageUrl"

}
