package com.github.lavrov.aired.importer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.github.lavrov.aired.maps.TileSystem
import com.github.lavrov.aired.protocol.StateUpdate
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

object Main extends App {

  val logger = LoggerFactory getLogger getClass

  implicit val system = ActorSystem("importer")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  val producerSettings =
    ProducerSettings(system, new StringSerializer, new StringSerializer)

  def restCall =
    for {
      response <-
        Http(system).singleRequest(
          HttpRequest(
            uri = Uri("https://opensky-network.org/api/states/all")))
      entity <-
        response.entity.toStrict(10.seconds)
    }
    yield {
      val bodyAsJson = Json.parse(entity.data.toArray)
      bodyAsJson.as[OpenSkyApi.Response]
    }


  val result =

    (Source(Stream continually restCall) zip Source.tick(10.seconds, 10.seconds, 1))
      .mapAsync(1) { case (response, _) => response }
      .mapConcat { response =>
        logger.debug(s"Received ${response.states.size} state updates")
        response.states
      }
      .map { stateUpdate =>
        val currentTime = System.currentTimeMillis()
        val kafkaMessage =
          StateUpdate(
            icao24 = stateUpdate.icao24,
            timestamp = currentTime,
            longitude = stateUpdate.longitude,
            latitude = stateUpdate.latitude,
            tile = TileSystem.fromLonLat(stateUpdate.longitude, stateUpdate.latitude, TileSystem.HighestDetailLevel)
          )
        new ProducerRecord(
          "StatusUpdates",
          kafkaMessage.icao24,
          Json.toJson(kafkaMessage).toString()
        )
      }
    .runWith(
      Producer.plainSink(producerSettings))

  Await.result(result, Duration.Inf)
  Await.result(system.terminate(), 10.seconds)

  logger.info("Finished")
}
