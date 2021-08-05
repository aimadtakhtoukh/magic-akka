package implementation.edition.scrappers.languages.extractor

import org.jsoup.nodes.Document

import scala.concurrent.{ExecutionContext, Future}

trait Extractor {

  def extract(doc : Document)(implicit ec : ExecutionContext) : Future[List[String]]

  def foreignNameFromSelector(selector : String)(doc : Document)(implicit ec : ExecutionContext) : Future[List[String]] =
    Future {
      Some(doc select selector)
        .filterNot(_.isEmpty)
        .map(_ get 0)
        .map(_.text())
        .filterNot(_.isEmpty)
        .toList
    }

}
