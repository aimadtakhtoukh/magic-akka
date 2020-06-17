package gatherer

import org.jsoup.nodes.{Document, Element}
import GathererScrapper.getLanguageDocuments
import Documents.{detailsUrl, getDocument, legalityUrl}

import scala.concurrent.{ExecutionContext, Future}

case class MidToDocuments(mid : String,
                          setCode : String,
                          detailsDocument : Document,
                          cardComponent : Element,
                          languageDocuments : List[Document],
                          legalityDocument : Document)

object MidToDocuments {
  def apply(cardName : String, mid : String, setCode : String)(implicit ec : ExecutionContext): Future[List[MidToDocuments]] =
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

case class Ruling(date : String, rule : String)
case class Legality(format : String, legality : String)

case class Language(multiverseId : String,
                    name: String,
                    types : List[String],
                    cardText : List[String],
                    flavorText : Option[String],
                    numberInSet : String,
                    language : String)

case class Card(mid : String,
                id : String,
                name : String,
                manaCost : Option[String],
                colors : List[String],
                colorIdentity : List[String],
                cmc : Option[String],
                types : String,
                superTypes : List[String],
                subTypes : List[String],
                cardText : List[String],
                flavorText : Option[String],
                power : Option[String],
                toughness : Option[String],
                loyalty : Option[String],
                expansion : String,
                rarity : String,
                cardNumberInSet : String,
                artist : String,
                editionCode : String,
                rulings : List[Ruling],
                legalities : List[Legality],
                languages: List[Language]
               )

