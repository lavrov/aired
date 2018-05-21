package com.github.lavrov.aired.processor

import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

object Main extends App {
  val config = ConfigFactory.load()

  val checkpointPath = "/tmp/aired_checkpoint"
  implicit val ctx = StreamingContext.getOrCreate(
    checkpointPath = checkpointPath,
    () => {
      val conf = new SparkConf()
        .setAppName("ingester")
        .set("spark.cassandra.connection.host", config.getString("cassandra.host"))
        .set("spark.cassandra.connection.port", config.getString("cassandra.port"))
      val result = new StreamingContext(conf, Seconds(10))
      result.checkpoint(checkpointPath)
      result
    }
  )

  val messagesFromKafka = PersistState.fromKafka(config.getString("kafka.bootstrap.servers")).persist()

  PersistState.toCassandra(messagesFromKafka)

  val trackingEvents = TileAggregations.toTrackingEvents(messagesFromKafka).persist()

  TileAggregations.updateTileVehicleView(trackingEvents)

  val tileCountUpdates = TileAggregations.toTileCountUpdates(trackingEvents)

  TileAggregations.updateTileCounts(tileCountUpdates)

  ctx.start()
  ctx.awaitTermination()
}

