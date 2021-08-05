package domain.gatherer

object Models {

  type RulingDate = String
  type RulingText = String

  case class Ruling(date: RulingDate, rule: RulingText)

  type Format = String

  sealed trait Legality
  case object Legal extends Legality
  case object Banned extends Legality
  case object Restricted extends Legality

  case class LegalityInFormat(format: Format, legality: Legality)

  case class Language(
    multiverseId: String,
    name: String,
    types: List[String],
    cardText: List[String],
    flavorText: Option[String],
    numberInSet: String,
    language: String
  )

  sealed trait Color
  case object White extends Color
  case object Blue extends Color
  case object Black extends Color
  case object Red extends Color
  case object Green extends Color

  case class Card(
    mid: String,
    id: String,
    name: String,
    manaCost: Option[String],
    colors: List[Color],
    colorIdentity: List[Color],
    cmc: Option[String],
    types: String,
    superTypes: List[String],
    subTypes: List[String],
    cardText: List[String],
    flavorText: Option[String],
    power: Option[String],
    toughness: Option[String],
    loyalty: Option[String],
    edition: String,
    rarity: String,
    cardNumberInSet: String,
    artist: String,
    editionCode: String,
    rulings: List[Ruling],
    legalities: List[LegalityInFormat],
    languages: List[Language]
  )

}
