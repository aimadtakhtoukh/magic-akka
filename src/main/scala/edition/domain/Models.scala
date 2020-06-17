package edition.domain

import java.time.{LocalDate, YearMonth}

object Models {
  type Language = String
  type SetName = String
  type SetCode = String

  case class EditionNames(englishName : SetName, names : Map[Language, SetName])

  case class MtgEdition(name : SetName = "",
                        code : SetCode = "",
                        secondaryCode : Option[SetCode] = None,
                        yearMonth : Option[YearMonth] = None,
                        releaseDate : Option[LocalDate] = None)


  case class GathererNameToCode(name : SetName, code : SetCode)

  case class EditionInfo(gathererName : SetName,
                         languageToNamesMap : Map[Language, SetName],
                         codes : List[SetCode],
                         yearMonth : YearMonth,
                         releaseDate : LocalDate)
}
