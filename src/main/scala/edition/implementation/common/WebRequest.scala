package edition.implementation.common

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

//Standard call to the url passed as a parameter.
class WebRequest(url: String)(implicit ec : ExecutionContext) {
  def requestToDocument(): Future[Document] = {
    Future {
      Try {
        (Jsoup connect url)
          .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0")
          .get()
      } match {
        case Success(doc) => doc
        case Failure(_) => Jsoup parse "<html></html>"
      }
    }
  }
}
