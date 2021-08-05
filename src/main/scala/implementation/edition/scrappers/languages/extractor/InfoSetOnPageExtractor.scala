package implementation.edition.scrappers.languages.extractor

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

object InfoSetOnPageExtractor extends Extractor {

  override def extract(doc: Document)(implicit ec: ExecutionContext): Future[List[String]] =
    Future {
      (doc select "#-tabs-1 p, #-tabs-1 blockquote, div.slides div.description, div.slides div.description blockquote")
        .asScala.toList
        .map(_.html())
        .map(_.replaceAll("<strong>(&nbsp;| )</strong>", ""))
        .map(_.replaceAll("<strong>(.{1,3})</strong>", "$1"))
        .map(_.replaceAll("<strong>", "<br><strong>"))
        .map(_.replaceAll("<b>", "<br><b>"))
        .map(_.replaceAll("<em> ?<br><strong>", "<em><strong>"))
        .flatMap(_.split("<br>"))
        .map(Jsoup.parse)
        .map(_.text().trim)
        .filter(c => setInfoForeignNameLabels().exists(c.startsWith))
        .map(c => setInfoForeignNameLabels().foldLeft(c)((acc, current) => acc.replaceAll(current, "")))
        .map(_.trim)
        .map(_.replaceAll("『FFrom", "『From"))
        .distinct
    }

  private def setInfoForeignNamePrefixes() : List[String] = List(
    "Set Name",
    "Nom de l’extension",
    "Nom de l'extension",
    "Nombre de la colección",
    "Название выпуска",
    "Название",
    "Nome da coleção",
    "Nome da Coleção",
    "세트 명칭",
    "세트명",
    "세트 명",
    "セット名",
    "Nome espansione",
    "Name des Sets",
    "系列名稱",
    "系列名称",
    "核心名称",
    "核心系列",
    "要系列名称"
  )

  private def setInfoForeignNameSuffixes() : List[String] = List(
    ":",
    " :",
    // Attention : charactères asiatiques
    "：",
    " ："
  )

  private def setInfoForeignNameLabels() : List[String] =
    setInfoForeignNamePrefixes().flatMap(prefix => setInfoForeignNameSuffixes().map(suffix => s"$prefix$suffix"))
}
