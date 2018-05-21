package com.github.lavrov.aired.webservice

import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, MalformedQueryParamRejection}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.OverflowStrategy
import akka.stream.QueueOfferResult.{Failure, QueueClosed}
import akka.stream.scaladsl.Source
import com.github.lavrov.aired.webservice.dao.{TileDao, VehicleDao}
import com.github.lavrov.aired.webservice.hot.TrajectoryKeeperApi
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observable
import play.api.libs.json.{Json, Reads, Writes}

import scala.concurrent.ExecutionContext

class Routes(
    vehicleDao: VehicleDao,
    trajectoryKeeperApi: TrajectoryKeeperApi,
    tileDao: TileDao,
    executionContext: ExecutionContext,
) {
  implicit val _ = executionContext
  val scheduler: Scheduler = monix.execution.Scheduler(executionContext)
  import TileDao._
  import VehicleDao._

  implicit def marshaller[A: Writes] = (
    Marshaller.stringMarshaller(`application/json`)
      compose Json.stringify
      compose implicitly[Writes[A]].writes
    )


  implicit def unmarshaller[A: Reads]: Unmarshaller[HttpEntity, A] =
    Unmarshaller.stringUnmarshaller.map { s =>
      Json.parse(s).as[A]
    }

  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  implicit val VehicleWriter = Json.writes[Vehicle]
  implicit val TileWriter = Json.writes[Tile]
  implicit val TileVehicleWriter = Json.writes[TileVehicle]
  implicit class ObservableAdapter[A](observable: Observable[A]) {
    def toSource: Source[A, _] = {
      Source.queue[A](100, OverflowStrategy.backpressure).mapMaterializedValue(
        queue =>
          observable.subscribe(
            nextFn = a => queue.offer(a).map {
              case QueueClosed | _: Failure => Ack.Stop
              case _ => Ack.Continue
            },
            errorFn = e => queue.fail(e),
            completedFn = () => queue.complete()
          )(scheduler)
      )
    }
  }

  val tileSetParameter: Directive1[Set[String]] = parameter('tile_list.as[String]).flatMap {
    raw =>
      raw.split(';').toList match {
        case Nil => reject(MalformedQueryParamRejection("tile_list", "Cannot be empty"))
        case nonEmpty =>
          if (nonEmpty.forall(_ matches "[0-3]+"))
            provide(nonEmpty.toSet)
          else
            reject(MalformedQueryParamRejection("tile_list", "Must be semicolon separated quadkey list"))
      }
  }

  def create =
    pathPrefix("api") {
      pathPrefix("vehicles") {
        path("list") {
          get {
            complete(vehicleDao.list)
          }
        } ~
        path(Segment / "lastPosition") { vehicleId =>
          get {
            complete(trajectoryKeeperApi get vehicleId)
          }
        }
      } ~
      pathPrefix("tiles") {
        path("filled") {
          get { complete(tileDao.filled) }
        } ~
        path("tile" / Segment / "availableVehicles") { tileId =>
          get {
            complete(tileDao.availableVehicles(tileId))
          }
        } ~
        path("usecase" / "vehicleCount") {
          tileSetParameter { tileIds =>
            get {
              complete(tileDao numberOfVehicles tileIds)
            }
          }
        }
      }
    }

}
