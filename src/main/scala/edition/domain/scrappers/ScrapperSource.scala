package edition.domain.scrappers

import akka.NotUsed
import akka.stream.scaladsl.Source

// Abstraction for a scrapper that sends elements downstream.
trait ScrapperSource[T] {
  def source : Source[T, NotUsed]
}
