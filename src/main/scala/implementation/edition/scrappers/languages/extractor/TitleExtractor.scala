package implementation.edition.scrappers.languages.extractor

import org.apache.commons.text.WordUtils
import org.jsoup.nodes.Document

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object TitleExtractor extends Extractor {

  private val removedWords = "Information" :: "Home" :: "home" :: "Preorder" :: Nil

  private val japaneseTitleRegex: Regex = "(『.*?』).*".r

  override def extract(doc: Document)(implicit ec : ExecutionContext) : Future[List[String]] =
    foreignNameFromSelector("title")(doc)
      .map(_.map(cleanTitle).collect {case Some(title) => title})

  private def cleanTitle(rawTitle : String) : Option[String] =
    Some(rawTitle)
      .map(_.split("( - )|[–|]")(0).trim)
      .map(title =>
        if (isUpperCase(title)) {
          WordUtils.capitalizeFully(title)
        } else {
          title
        }
      )
      .map(removedWords.fold(_)((title, removedWord) => title.replace(removedWord, "")))
      .map {
        case japaneseTitleRegex(title) => title
        case title => title
      }
      .filterNot(_.isEmpty)

  def isUpperCase(s: String): Boolean = {
    for (i <- 0 until s.length) {
      if (Character.isLowerCase(s.charAt(i))) return false
    }
    true
  }
}
