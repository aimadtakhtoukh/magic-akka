package domain.edition

import java.time.{LocalDate, YearMonth}

object Models {
  trait DomainModel

  type Language = String
  type SetName = String
  type SetCode = String

  case class EditionNames(englishName: SetName, names: Map[Language, SetName]) extends DomainModel

  case class MtgEdition(name: SetName = "",
    code: SetCode = "",
    secondaryCode: Option[SetCode] = None,
    yearMonth: Option[YearMonth] = None,
    releaseDate: Option[LocalDate] = None
  ) extends DomainModel

  case class GathererNameToCode(name: SetName, code: SetCode) extends DomainModel

  case class EditionInfo(gathererName: SetName,
    languageToNamesMap: Map[Language, SetName],
    codes: List[SetCode],
    yearMonth: YearMonth,
    releaseDate: LocalDate
  ) extends DomainModel
}
