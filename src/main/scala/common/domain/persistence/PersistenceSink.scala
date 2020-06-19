package common.domain.persistence

import akka.Done
import akka.stream.scaladsl.Sink

import scala.concurrent.Future

// Abstraction for persistence out of an Akka stream.
trait PersistenceSink[T] {
  def sink: Sink[T, Future[Done]]
}
