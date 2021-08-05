package implementation.gatherer.mongo

import domain.gatherer.Models
import domain.gatherer.Models.{Banned, Black, Blue, Green, Legal, Red, Restricted, White}

object MongoDTO {
  trait MongoModel

  case class Ruling(date: String, rule: String) extends MongoModel

  object Ruling {
    def fromModel(domain: Models.Ruling) : Ruling =
      Ruling(date = domain.date, rule = domain.rule)

    def toModel(dto: Ruling) : Models.Ruling =
      Models.Ruling(date = dto.date, rule = dto.rule)
  }

  case class LegalityInFormat(format: String, legality: String) extends MongoModel

  object Legality {
    def fromModel(domain: Models.Legality) : String = domain match {
      case Legal => "Legal"
      case Banned => "Banned"
      case Restricted => "Restricted"
    }

    def toModel(dto: String) : Models.Legality = dto match {
      case "Legal" => Legal
      case "Banned" => Banned
      case "Restricted" => Restricted
    }
  }

  object LegalityInFormat {
    def fromModel(domain: Models.LegalityInFormat) : LegalityInFormat =
      LegalityInFormat(
        format = domain.format,
        legality = Legality.fromModel(domain.legality)
      )

    def toModel(dto: LegalityInFormat) : Models.LegalityInFormat =
      Models.LegalityInFormat(
        format = dto.format,
        legality = Legality.toModel(dto.legality)
      )
  }

  case class Language(
    multiverseId: String,
    name: String,
    types: List[String],
    cardText: List[String],
    flavorText: Option[String],
    numberInSet: String,
    language: String
  ) extends MongoModel

  object Language {
    def fromModel(domain: Models.Language) : Language =
      Language(
        multiverseId = domain.multiverseId,
        name = domain.name,
        types = domain.types,
        cardText = domain.cardText,
        flavorText = domain.flavorText,
        numberInSet = domain.numberInSet,
        language = domain.language
      )

    def toModel(dto: Language) : Models.Language =
      Models.Language(
        multiverseId = dto.multiverseId,
        name = dto.name,
        types = dto.types,
        cardText = dto.cardText,
        flavorText = dto.flavorText,
        numberInSet = dto.numberInSet,
        language = dto.language
      )
  }

  object Color {
    def fromModel(domain: Models.Color) : String = domain match {
      case White => "W"
      case Blue => "U"
      case Black => "B"
      case Red => "R"
      case Green => "G"
    }

    def toModel(dto: String) : Models.Color = dto match {
      case "U" => Blue
      case "W" => White
      case "B" => Black
      case "R" => Red
      case "G" => Green
    }
  }

  case class Card(mid: String,
    id: String,
    name: String,
    manaCost: Option[String],
    colors: List[String],
    colorIdentity: List[String],
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
  ) extends MongoModel

  object Card {
    def fromModel(domain: Models.Card) : Card =
      Card(
        mid = domain.mid,
        id = domain.id,
        name = domain.name,
        manaCost = domain.manaCost,
        colors = domain.colors.map(Color.fromModel),
        colorIdentity = domain.colorIdentity.map(Color.fromModel),
        cmc = domain.cmc,
        types = domain.types,
        superTypes = domain.superTypes,
        subTypes = domain.subTypes,
        cardText = domain.cardText,
        flavorText = domain.flavorText,
        power = domain.power,
        toughness = domain.toughness,
        loyalty = domain.loyalty,
        edition = domain.edition,
        rarity = domain.rarity,
        cardNumberInSet = domain.cardNumberInSet,
        artist = domain.artist,
        editionCode = domain.editionCode,
        rulings = domain.rulings.map(Ruling.fromModel),
        legalities = domain.legalities.map(LegalityInFormat.fromModel),
        languages = domain.languages.map(Language.fromModel)
      )

    def toModel(dto: Card) : Models.Card = {
      Models.Card(
        mid = dto.mid,
        id = dto.id,
        name = dto.name,
        manaCost = dto.manaCost,
        colors = dto.colors.map(Color.toModel),
        colorIdentity = dto.colorIdentity.map(Color.toModel),
        cmc = dto.cmc,
        types = dto.types,
        superTypes = dto.superTypes,
        subTypes = dto.subTypes,
        cardText = dto.cardText,
        flavorText = dto.flavorText,
        power = dto.power,
        toughness = dto.toughness,
        loyalty = dto.loyalty,
        edition = dto.edition,
        rarity = dto.rarity,
        cardNumberInSet = dto.cardNumberInSet,
        artist = dto.artist,
        editionCode = dto.editionCode,
        rulings = dto.rulings.map(Ruling.toModel),
        legalities = dto.legalities.map(LegalityInFormat.toModel),
        languages = dto.languages.map(Language.toModel)
      )
    }
  }
}
