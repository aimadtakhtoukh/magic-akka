package edition.implementation.persistance.mongo

import akka.Done
import akka.stream.scaladsl.Sink
import org.mongodb.scala.MongoCollection
import edition.domain.persistance.PersistenceSink

import scala.concurrent.Future

class MongoPersistenceSink[T](mongoCollection : MongoCollection[T]) extends PersistenceSink[T] {
  override def sink: Sink[T, Future[Done]] = Sink.foreach(mongoCollection.insertOne(_).foreach(_ => ()))
}
