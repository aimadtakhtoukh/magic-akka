package gatherer.domain

object Models {

  type RulingDate = String
  type RulingText = String
  case class Ruling(date : RulingDate, rule : RulingText)

  type Format = String
  object Legality extends Enumeration {
    val Legal, Banned = Value
  }
  case class LegalityInFormat(format : Format, legality : Legality.Value)

  case class Language(multiverseId : String,
                      name: String,
                      types : List[String],
                      cardText : List[String],
                      flavorText : Option[String],
                      numberInSet : String,
                      language : String)

  object Color extends Enumeration {
    val White: Color.Value = Value("W")
    val Blue: Color.Value = Value("U")
    val Black: Color.Value = Value("B")
    val Red: Color.Value = Value("R")
    val Green: Color.Value = Value("G")
  }

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
                  edition : String,
                  rarity : String,
                  cardNumberInSet : String,
                  artist : String,
                  editionCode : String,
                  rulings : List[Ruling],
                  legalities : List[LegalityInFormat],
                  languages: List[Language]
                 )
}

