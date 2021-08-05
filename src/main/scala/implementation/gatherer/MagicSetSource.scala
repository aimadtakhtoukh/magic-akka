package implementation.gatherer

import akka.NotUsed
import akka.actor.Scheduler
import akka.stream.scaladsl.Source
import implementation.gatherer.Documents.{gathererDefaultPageUrl, getDocument}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

object MagicSetSource {

  def magicSetList(implicit ec: ExecutionContext, s: Scheduler) : Future[List[String]] =
    getDocument(gathererDefaultPageUrl).map(
      _.getElementById("ctl00_ctl00_MainContent_Content_SearchControls_setAddText")
      .children
      .asScala
      .map(_.attr("value"))
      .filterNot(_.isEmpty)
      .toList
  )

  def allMagicSetSource(implicit ec: ExecutionContext, s: Scheduler) : Source[String, NotUsed] = Source.future(magicSetList).flatMapConcat(Source(_))

  def magicSetFromString(firstSet : String, sets : String*) : Source[String, NotUsed] = Source(firstSet :: sets.toList)

}
