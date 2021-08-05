package implementation.edition.server.curation

//Set names to slug. Lots of set specific code.
class MagicCardSetSluggifier(s: String) {
  val removedWords = List("limited", "edition", "duel decks:", "core set", "magic: the gathering")
  def slug(): String = Option(s)
    .map(_.toLowerCase)
    .map(_.replaceAll("[()]", ""))
    .map(title => removedWords.foldLeft(title)((title, removed) => title.replaceAll(removed, "")))
    .map(_.replaceAll("magic ([0-9]*)", "$1"))
    .map(_.replaceAll("duel decks anthology, .*", "duel decks anthology"))
    .map(_.replaceAll("guild kit: .*", "guild kits"))
    .map(_.replaceAll("-commander", "commander_2011"))
    .map(_.replaceAll("^planechase$", "planechase_2009"))
    .map(_.replaceAll(" (?:and|vs\\.) ", " "))
    .map(_.trim)
    .map(_.replaceAll(" ", "_"))
    .map(_.replaceAll("&", "and"))
    .map(_.replaceAll("((?![a-z0-9_]).)*", ""))
    .map(_.replaceAll("^_*?(.*?)^_?$", "$1"))
    .getOrElse("")
}
