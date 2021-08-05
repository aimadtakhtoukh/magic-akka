package implementation.gatherer

import akka.actor.Scheduler
import akka.pattern.after
import org.jsoup.Connection.Method
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object Documents {
  val gathererUrl = "http://gatherer.wizards.com"
  val gathererPageUrl = s"$gathererUrl/Pages"
  val gathererDefaultPageUrl = s"$gathererPageUrl/Default.aspx"
  val gathererCardUrl = s"$gathererPageUrl/Card"

  def detailsUrl(multiverseId: String): String = s"$gathererCardUrl/Details.aspx?multiverseid=$multiverseId"

  def languageUrl(multiverseId: String): String = s"$gathererCardUrl/Languages.aspx?multiverseid=$multiverseId"

  def printedCardUrl(multiverseId: String): String = s"$gathererCardUrl/Details.aspx?printed=true&multiverseid=$multiverseId"

  def legalityUrl(multiverseId: String) = s"$gathererCardUrl/Printings.aspx?multiverseid=$multiverseId"

  def imageUrl(multiverseId: String): String = s"$gathererUrl/Handlers/Image.ashx?type=card&multiverseid=$multiverseId"

  def getDocument(url: String)(implicit ec: ExecutionContext, s: Scheduler): Future[Document] = {
    def baseCall: Future[Document] =
      Future {
        Jsoup.connect(url)
          .header("Accept-Language", "en")
          .cookie("CardDatabaseSettings", "0=1&1=28&2=0&14=1&3=13&4=0&5=0&6=15&7=1&8=0&9=1&10=16&11=7&12=8&15=1&16=0&13=")
          .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:41.0) Gecko/20100101 Firefox/41.0")
          .ignoreHttpErrors(true)
          .timeout(60 * 1000)
          .maxBodySize(0)
          .method(Method.GET)
          .execute()
          .parse()
      }

    def retry[T](f: => Future[T], delay: FiniteDuration, retries: Int)(implicit ec: ExecutionContext, s: Scheduler): Future[T] = {
      f recoverWith { case _ if retries > 0 => after(delay, s)(retry(f, delay, retries - 1)) }
    }

    retry(baseCall, 10 seconds, 10)

  }

}
