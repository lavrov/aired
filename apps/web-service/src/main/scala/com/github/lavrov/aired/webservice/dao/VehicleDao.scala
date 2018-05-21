package com.github.lavrov.aired.webservice.dao

import java.sql.Timestamp
import java.time.Instant

import com.github.lavrov.aired.webservice.utils.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

class VehicleDao(
    cassandraSession: CassandraSession,
    executionContext: ExecutionContext
) {
  import VehicleDao._
  implicit val _ = executionContext

  def list: Future[List[Vehicle]] = {
    import cassandraSession.async._
    run {
      val selectAllVehicles = query[Vehicle]
      val oneRowPerPartition = infix"$selectAllVehicles PER PARTITION LIMIT 1".as[Query[Vehicle]]
      oneRowPerPartition
    }
  }

  def lastPosition(vehicleId: String): Future[Option[Vehicle]] = {
    import cassandraSession.async._
    run {
      query[Vehicle]
        .filter(_.id == lift(vehicleId))
        .take(1)
    }
    .map(_.headOption)
  }
}

object VehicleDao {
  case class Vehicle(
      id: String,
      timestamp: Instant,
      longitude: Double,
      latitude: Double,
      tile: String
  )
}