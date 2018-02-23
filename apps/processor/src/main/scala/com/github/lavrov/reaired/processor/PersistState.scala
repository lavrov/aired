package com.github.lavrov.aired.processor

import com.datastax.spark.connector._
import com.github.lavrov.aired.protocol.StateUpdate
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import play.api.libs.json.Json

object PersistState {

  def fromKafka(servers: String)(implicit ctx: StreamingContext): DStream[StateUpdate] = {
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> servers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "ingester",
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val rawStream =
      KafkaUtils.createDirectStream(
        ctx,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](Seq("StateUpdates"), kafkaParams)
      )

    rawStream.map(record => readMessage(record.value()))
  }

  def toCassandra(source: DStream[StateUpdate]) = {
    source.foreachRDD { rdd =>
      rdd.saveToCassandra("aired", "vehicle",
        SomeColumns(
          "id" as "vehicleId",
          "timestamp",
          "longitude",
          "latitude",
          "tile"
        ))
    }
  }

  private[processor] def readMessage(input: String) = Json.parse(input).as[StateUpdate]

}
