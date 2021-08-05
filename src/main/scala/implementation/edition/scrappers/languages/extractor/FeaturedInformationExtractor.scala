package implementation.edition.scrappers.languages.extractor

import org.jsoup.nodes.Document

import scala.concurrent.{ExecutionContext, Future}

object FeaturedInformationExtractor extends Extractor {

  override def extract(doc: Document)(implicit ec : ExecutionContext) : Future[List[String]] =
    foreignNameFromSelector(".module_product-featured--information p span i,.module_product-featured--information p span em")(doc)
      .map(_.map(_.replace("Dominaira", "Dominaria")))

}
