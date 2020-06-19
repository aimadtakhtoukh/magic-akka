package edition.implementation.persistance.mongo

import com.typesafe.config.{Config, ConfigFactory}
import edition.domain.Models.{EditionInfo, EditionNames, GathererNameToCode, MtgEdition}
import common.implementation.persistence.mongo.codec.YearMonthCodec
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries, fromCodecs}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

object MongoRepository {
  private val config : Config = ConfigFactory.load()

  private val codecRegistry = fromRegistries(
    fromProviders(
      classOf[EditionNames],
      classOf[MtgEdition],
      classOf[GathererNameToCode],
      classOf[EditionInfo]
    ),
    fromCodecs(new YearMonthCodec()),
    DEFAULT_CODEC_REGISTRY
  )

  private val client = MongoClient(config.getString("mongo.url"))
  val batchesDb: MongoDatabase = client.getDatabase("batches").withCodecRegistry(codecRegistry)
  def editionNamesCollection: MongoCollection[EditionNames] = batchesDb.getCollection("editionNames")
  def mtgEditionCollection: MongoCollection[MtgEdition] = batchesDb.getCollection("mtgEdition")
  def gathererCodeCollection: MongoCollection[GathererNameToCode] = batchesDb.getCollection("gathererCode")

  val magicDb : MongoDatabase = client.getDatabase("magic").withCodecRegistry(codecRegistry)
  def editionCollection: MongoCollection[EditionInfo] = magicDb.getCollection("edition")
}
