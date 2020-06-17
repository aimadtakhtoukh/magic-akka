package gatherer

import akka.Done
import akka.stream.scaladsl.Sink
import edition.domain.persistance.PersistenceSink
import org.mongodb.scala.MongoCollection

import scala.concurrent.Future

class MongoPersistenceSink[T](mongoCollection : MongoCollection[T]) {
  def sink: Sink[T, Future[Done]] = Sink.foreach(mongoCollection.insertOne(_).foreach(_ => ()))
}
