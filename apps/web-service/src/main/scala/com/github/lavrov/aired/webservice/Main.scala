package com.github.lavrov.aired.webservice

import akka.actor.ActorSystem
import akka.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.kafka.ConsumerSettings
import akka.stream.ActorMaterializer
import com.github.lavrov.aired.protocol.StateUpdate
import com.github.lavrov.aired.webservice.dao.{TileDao, VehicleDao}
import com.github.lavrov.aired.webservice.hot.{TrajectoryKeeper, TrajectoryKeeperApi}
import com.github.lavrov.aired.webservice.utils.{CassandraSession, JsonReadableDeserializer, StatusUpdatesConsumerSettings}
import com.softwaremill.macwire.wire
import com.typesafe.config.ConfigFactory
import io.getquill.CassandraContextConfig
import org.apache.kafka.common.serialization.StringDeserializer

object Main extends App {

  implicit lazy val system = ActorSystem("web-service")
  implicit lazy val typedSystem = system.toTyped
  implicit lazy val scheduler = typedSystem.scheduler
  implicit lazy val executionContext = system.dispatcher
  implicit lazy val materializer = ActorMaterializer()

  lazy val config = ConfigFactory.load()

  lazy val cassandraContextConfig = CassandraContextConfig(config getConfig "cassandra")
  lazy val cassandraBootstrap = wire[CassandraBootstrap]
  lazy val cassandraSession = new CassandraSession(cassandraContextConfig)

  lazy val vehicleDao = wire[VehicleDao]
  lazy val tileDao = wire[TileDao]
  lazy val statusUpdatesConsumerSettings =
    StatusUpdatesConsumerSettings(
      ConsumerSettings(system, new StringDeserializer, new JsonReadableDeserializer[StateUpdate])
    )
  lazy val trajectoryKeeperCoordinator = wire[TrajectoryKeeper.Coordinator]
  lazy val trajectoryKeeperApi = wire[TrajectoryKeeperApi]
  lazy val routes: Routes = wire[Routes]

  try {
    cassandraBootstrap.schema()
    trajectoryKeeperCoordinator.subscribe(statusUpdatesConsumerSettings)
    val bindingFuture = Http().bindAndHandle(routes.create, "0.0.0.0", 9000)

    bindingFuture.foreach { _ =>
      println(s"Server online at 0.0.0.0:9000/\nPress RETURN to stop...")
    }

    Runtime.getRuntime addShutdownHook new Thread(
      () =>
        for {
          binding <- bindingFuture
          _ <- binding.unbind()
          _ <- system.terminate()
        }
          yield {
            println("Server stopped.")
            System.exit(0)
          }
    )
  }
  catch {
    case e: Throwable =>
      e.printStackTrace()
      println("Failed to start the server.")
      System.exit(1)
  }
}
