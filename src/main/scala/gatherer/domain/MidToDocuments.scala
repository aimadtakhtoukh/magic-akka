package gatherer.domain

import gatherer.implementation.Documents.{detailsUrl, getDocument, legalityUrl}
import gatherer.GathererScrapper.getLanguageDocuments
import org.jsoup.nodes.{Document, Element}

import scala.concurrent.{ExecutionContext, Future}

object midToDocuments {
  type MultiverseId = String
  type SetCode = String

  case class MidToDocuments(mid : MultiverseId,
                            setCode : SetCode,
                            detailsDocument : Document,
                            cardComponent : Element,
                            languageDocuments : List[Document],
                            legalityDocument : Document)

  object MidToDocuments {
    def apply(cardName : String, mid : MultiverseId, setCode : SetCode)(implicit ec : ExecutionContext): Future[List[MidToDocuments]] =
      for {
        detailsDocument <- getDocument(detailsUrl(mid))
        languageDocument <- getLanguageDocuments(mid)
        legalityDocument <- getDocument(legalityUrl(mid))
      } yield {
        import scala.jdk.CollectionConverters._
        detailsDocument
          .select(".cardComponentContainer")
          .asScala
          .toList
          .filter(container => container.select("[id$=nameRow] .value").text().equals(cardName))
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
