package edition.implementation.scrappers.gatherer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import gatherer.{GathererScrapper, MagicSetSource}
import edition.domain.Models.GathererNameToCode
import edition.domain.scrappers.ScrapperSource
import edition.implementation.common.WebRequest

import scala.concurrent.ExecutionContextExecutor
import scala.language.implicitConversions

class MagicCardSetGathererScrapperSource(implicit system: ActorSystem, executionContextExecutor: ExecutionContextExecutor)
  extends ScrapperSource[GathererNameToCode] {

  implicit def webRequestFromUrl(url : String) : WebRequest = new WebRequest(url)

  def source: Source[GathererNameToCode, NotUsed] =
    MagicSetSource.allMagicSetSource
      .map(edition => (edition, GathererScrapper.getFullSetUrl(edition)))
      .mapAsyncUnordered(4) { case (name, url) => url.requestToDocument().map(name -> _)}
      .map {
        case (name, document) =>
          Option(document.selectFirst("table tr.cardItem td.printings a img"))
            .map(_.attr("alt"))
            .map(name -> _)
      }
      .map(_.map(GathererNameToCode.tupled))
      .recover {
        case ex =>
          ex.printStackTrace()
          None
      }
      .collect {case Some(c) => c}
      .filterNot(_.name.isEmpty)
      .filterNot(_.name == "Vanguard")
      .filterNot(_.name == "Promo set for Gatherer")
}
