package common.implementation.persistence.mongo

import akka.Done
import akka.stream.scaladsl.Sink
import common.domain.persistence.PersistenceSink
import org.mongodb.scala.MongoCollection

import scala.concurrent.Future

class MongoPersistenceSink[T](mongoCollection : MongoCollection[T]) extends PersistenceSink[T] {
  override def sink: Sink[T, Future[Done]] = Sink.foreach(mongoCollection.insertOne(_).foreach(_ => ()))
}
