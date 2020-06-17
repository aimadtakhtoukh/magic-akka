package gatherer

import akka.NotUsed
import akka.stream.scaladsl.Source
import Documents.{gathererDefaultPageUrl, getDocument}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

object MagicSetSource {

  def magicSetList(implicit ec: ExecutionContext) : Future[List[String]] =
    getDocument(gathererDefaultPageUrl).map(
      _.getElementById("ctl00_ctl00_MainContent_Content_SearchControls_setAddText")
      .children
      .asScala
      .map(_.attr("value"))
      .filterNot(_.isEmpty)
      .toList
  )

  def allMagicSetSource(implicit ec: ExecutionContext) : Source[String, NotUsed] = Source.future(magicSetList).flatMapConcat(Source(_))

  def magicSetFromString(sets : List[String]) : Source[String, NotUsed] = Source(sets)

}
