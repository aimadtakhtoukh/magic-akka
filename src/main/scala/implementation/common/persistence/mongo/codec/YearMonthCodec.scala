package implementation.common.persistence.mongo.codec

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

import java.time.YearMonth

class YearMonthCodec extends Codec[YearMonth] {
  override def encode(writer: BsonWriter, value: YearMonth, encoderContext: EncoderContext): Unit = {
    writer.writeStartDocument()
    writer.writeName("year")
    writer.writeInt32(value.getYear)
    writer.writeName("month")
    writer.writeInt32(value.getMonth.getValue)
    writer.writeEndDocument()
  }

  override def getEncoderClass: Class[YearMonth] = classOf[YearMonth]

  override def decode(reader: BsonReader, decoderContext: DecoderContext): YearMonth = {
    reader.readStartDocument()
    val year = reader.readInt32("year")
    val month = reader.readInt32("month")
    reader.readEndDocument()
    YearMonth.of(year, month)
  }
}
