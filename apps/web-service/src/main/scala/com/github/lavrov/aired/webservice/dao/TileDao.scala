package com.github.lavrov.aired.webservice.dao

import com.github.lavrov.aired.webservice.utils.CassandraSession
import monix.reactive.Observable

import scala.concurrent.{ExecutionContext, Future}

class TileDao(
    cassandraComponent: CassandraSession,
    executionContext: ExecutionContext
) {
  import TileDao._
  implicit val _ = executionContext

  def filled: Future[List[String]] = {
    import cassandraComponent.async._
    run {
      query[Tile].filter(_.numberOfVehicles > 0).map(_.id).allowFiltering
    }
  }

  def availableVehicles(tileId: String): Future[List[String]] = {
    import cassandraComponent.async._

    val like = quote {
      (field: String, value: String) =>
        infix"$field LIKE $value".as[Boolean]
    }

    if (tileId.length == 23)
      run {
        query[TileVehicle]
          .filter(_.tileId == lift(tileId))
          .map(_.vehicleId)
      }
    else
      run {
        query[TileVehicle]
          .filter(row => like(row.tileIdIdx, lift(tileId + '%')))
          .map(_.vehicleId)
      }
  }

  def numberOfVehicles(tileIds: Set[String]): Future[List[Tile]] =  {
    import cassandraComponent.async._
    run {
      query[Tile]
        .filter(tile => liftQuery(tileIds).contains(tile.id))
    }
  }

}

object TileDao {
  case class Tile(
      id: String,
      numberOfVehicles: Long
  )

  case class TileVehicle(
      tileId: String,
      vehicleId: String,
      tileIdIdx: String
  )
}

