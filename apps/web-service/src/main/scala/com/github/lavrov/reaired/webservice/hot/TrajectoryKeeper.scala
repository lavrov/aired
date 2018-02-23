package com.github.lavrov.aired.webservice.hot

import java.time.Instant

import akka.actor.Scheduler
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.Materializer
import akka.typed.{ActorRef, ActorSystem, Behavior, Props}
import akka.typed.scaladsl.AskPattern._
import akka.typed.cluster.sharding.{ClusterSharding, ClusterShardingSettings, EntityTypeKey, ShardingEnvelope}
import akka.typed.scaladsl.Actor
import akka.util.Timeout
import com.github.lavrov.aired.webservice.dao.VehicleDao.Vehicle
import com.github.lavrov.aired.webservice.utils.StatusUpdatesConsumerSettings
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object TrajectoryKeeper {
  val logger = LoggerFactory getLogger getClass
  type State = List[Vehicle]
  sealed trait Command
  case class UpdatePosition(vehicle: Vehicle) extends Command
  case class Get(replyTo: ActorRef[State]) extends Command
  case object Get
  case object Passivate extends Command

  val TypeKey = EntityTypeKey[Command]("vehicle_trajectory")

  def behaviour[A](trajectory: List[Vehicle] = Nil): Behavior[Command] =
    Actor.immutable {
      (_, msg) =>
        msg match {
          case UpdatePosition(current) =>
            behaviour((current :: trajectory) take 10) // naive solution
          case Get(replyTo) =>
            replyTo ! trajectory
            Behavior.same
          case Passivate =>
            Behavior.stopped
        }
    }

  class Coordinator(system: ActorSystem[_]) {
    val logger = LoggerFactory getLogger getClass

    val ref = {
      val settings = ClusterShardingSettings(system)
      ClusterSharding(system).spawn[Command](
        behaviour(),
        Props.empty,
        TypeKey,
        settings.withTuningParameters(
          settings.tuningParameters.withHandOffTimeout(10.minute)),
        100,
        Passivate
      )
    }

    def subscribe(consumerSettings: StatusUpdatesConsumerSettings)(implicit mat: Materializer, scheduler: Scheduler) = {
      Consumer.plainSource(
        consumerSettings.settings,
        Subscriptions.topics("StatusUpdates")
      )
        .runForeach { record =>
          val message = record.value()
          val position = Vehicle(message.icao24, Instant ofEpochMilli message.timestamp, message.longitude, message.latitude, message.tile)
          val envelope = ShardingEnvelope(message.icao24, UpdatePosition(position): Command)
//          logger.debug(s"Sending to shard coordinator $message")
          ref ! envelope
        }
    }
  }
}

class TrajectoryKeeperApi(
    coordinator: TrajectoryKeeper.Coordinator,
    scheduler: Scheduler
) {
  implicit val timeout = Timeout(10.seconds)
  implicit val _scheduler = scheduler

  def get(vehicleId: String): Future[TrajectoryKeeper.State] = {
    coordinator.ref ? {
      replyTo =>
        ShardingEnvelope(vehicleId, TrajectoryKeeper.Get(replyTo))
    }
  }

}
