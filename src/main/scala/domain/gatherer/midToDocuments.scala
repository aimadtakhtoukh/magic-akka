package domain.gatherer

import akka.actor.Scheduler
import implementation.gatherer.GathererScrapper.getLanguageDocuments
import implementation.gatherer.Documents.{detailsUrl, getDocument, legalityUrl}
import org.jsoup.nodes.{Document, Element}

import scala.concurrent.{ExecutionContext, Future}

object midToDocuments {
  type MultiverseId = String
  type SetCode = String

  case class MidToDocuments(mid: MultiverseId,
    setCode: SetCode,
    detailsDocument: Document,
    cardComponent: Element,
    languageDocuments: List[Document],
    legalityDocument: Document)

  object MidToDocuments {
    def apply(cardName: String, mid: MultiverseId, setCode: SetCode)(implicit ec: ExecutionContext, s: Scheduler): Future[List[MidToDocuments]] = {
      import scala.jdk.CollectionConverters._
      for {
        detailsDocument <- getDocument(detailsUrl(mid))
        languageDocument <- getLanguageDocuments(mid)
        legalityDocument <- getDocument(legalityUrl(mid))
      } yield {
        detailsDocument
          .select(".cardComponentContainer")
          .asScala
          .toList
          .filter(_.select("[id$=nameRow] .value").text().equals(cardName))
          .map(
            MidToDocuments(
              mid,
              setCode,
              detailsDocument,
              _,
              languageDocument,
              legalityDocument
            )
          )
      }
    }
  }

}
