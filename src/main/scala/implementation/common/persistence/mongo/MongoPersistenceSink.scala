package implementation.common.persistence.mongo

import akka.Done
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import domain.common.persistence.PersistenceSink
import org.mongodb.scala.MongoCollection

import scala.concurrent.Future

class MongoPersistenceSink[T](mongoCollection : MongoCollection[T]) extends PersistenceSink[T] {
  override def sink: Sink[T, Future[Done]] = {
    Flow
      .fromFunction {t : T => (mongoCollection.insertOne(t), t) }
      .flatMapConcat {case (observable, t: T) => Source.fromPublisher(observable).map(_ => t)}
      .toMat(Sink.ignore)(Keep.right)
  }
}
