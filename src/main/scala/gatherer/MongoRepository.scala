package gatherer

import com.typesafe.config.{Config, ConfigFactory}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

object MongoRepository {
  private val config : Config = ConfigFactory.load()

  private val codecRegistry = fromRegistries(
    fromProviders(
      classOf[Ruling],
      classOf[Legality],
      classOf[Language],
      classOf[Card]
    ),
    DEFAULT_CODEC_REGISTRY
  )
  private val client = MongoClient(config.getString("mongo.url"))

  val magicDb : MongoDatabase = client.getDatabase("magic").withCodecRegistry(codecRegistry)
  def cardCollection: MongoCollection[Card] = magicDb.getCollection("card")
}
