package implementation.edition.scrappers.date

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, YearMonth}
import java.util.Locale

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import org.jsoup.nodes.Document
import implementation.edition.common.WebRequest
import domain.edition.Models.MtgEdition
import domain.edition.scrappers.ScrapperSource

import scala.concurrent.ExecutionContextExecutor
import scala.jdk.CollectionConverters._
import scala.language.implicitConversions
import scala.util.Try

class MagicCardSetDateScrapperSource(implicit system: ActorSystem, executionContextExecutor: ExecutionContextExecutor)
  extends ScrapperSource[MtgEdition] {

  implicit def webRequestFromUrl(url : String) : WebRequest = new WebRequest(url)
  private implicit def yearMonthConverter(dateString : String) : YearMonthConverter = new YearMonthConverter(dateString)
  private implicit def releaseDateExtractor(document: Document) : MtgReleaseDateExtractor = new MtgReleaseDateExtractor(document)

  private val domain = "https://mtg.gamepedia.com"
  private val setCodeRegex = "(.*?)\\((.*?)\\)".r

  def source: Source[MtgEdition, NotUsed] =
    Source.future(s"$domain/Set".requestToDocument())
      .mapConcat(document => (document select "table.wikitable.sortable.mw-collapsible > tbody tr").asScala.toList)
      .filterNot(_.select("td:nth-child(6)").text().contains("Magic Encyclopedia"))
      .mapAsync(4)(
        c => {
          val (first : String, second : Option[String]) = c.select("td:nth-child(4)").text() match {
            case setCodeRegex(f, s) => (f.trim, Some(s).map(_.trim))
            case default => (default.trim, None)
          }
          val link = domain + c.select("td:nth-child(2) a").attr("href")
          val yearMonth = c.select("td:nth-child(1)").text().convertToYearMonth()
          link.requestToDocument()
            .map(_.extractReleaseDate(yearMonth))
            .map(releaseDate => MtgEdition(
              name = c.select("td:nth-child(2) i").text(),
              code = first,
              secondaryCode = second,
              yearMonth = yearMonth,
              releaseDate = releaseDate
            ))
        }
      )
      .filterNot(_.name.isEmpty)

  class YearMonthConverter(dateString : String) {
    def convertToYearMonth() : Option[YearMonth] =
      Try {YearMonth.parse(dateString.split(" ")(0), DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH))}.toOption
  }

  class MtgReleaseDateExtractor(doc : Document) {
    implicit def dateConverter(date : Option[String]): DateConverter = new DateConverter(date)
    private val dateInSideArrayRegex = "((?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{1,2}, \\d{4}).*?".r
    private val dateInTextRregex = "(?:released on) ((?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{1,2}, \\d{4})".r

    private def dateFromSideArray: Option[LocalDate] =
      doc
        .select(".mw-parser-output > table tr").asScala
        .filter(_.text().contains("Release date"))
        .map(_.selectFirst("td").text() )
        .headOption
        .flatMap {
          case dateInSideArrayRegex(date) => Some(date)
          case _ => None
        }
        .flatMap(c => Try {LocalDate.parse(c, DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH))}.toOption)

    private def dateFromText(yearMonth: Option[YearMonth]) : Option[LocalDate] =
      dateInTextRregex
        .findAllMatchIn(doc.text().replaceAll("\n", " "))
        .map(_.group(1))
        .toSet
        .flatMap((s : String) => Some(s).convertToDate())
        .find(date => yearMonth.exists(YearMonth.from(date).equals(_)))

    private def dateDeducedFromYearMonth(yearMonth: Option[YearMonth]) : Option[LocalDate] =
      yearMonth.map(_.atDay(1))

    def extractReleaseDate(yearMonth: Option[YearMonth]) : Option[LocalDate] =
      dateFromSideArray.orElse(
        dateFromText(yearMonth).orElse(
          dateDeducedFromYearMonth(yearMonth)
        )
      )

    class DateConverter(dateString : Option[String]) {
      def convertToDate() : Option[LocalDate] = dateString.flatMap(c => {
        val fullDate = Try {LocalDate.parse(c, DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH))}.toOption
        val monthAndYearDate = Try {YearMonth.parse(c, DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))}.map(_.atEndOfMonth()).toOption
        fullDate.orElse(monthAndYearDate)
      })
    }
  }
}
