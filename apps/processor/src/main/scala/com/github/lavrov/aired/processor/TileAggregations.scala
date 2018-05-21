package com.github.lavrov.aired.processor

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import com.github.lavrov.aired.maps.TileSystem
import com.github.lavrov.aired.protocol.StateUpdate
import org.apache.spark.SparkContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Minutes, State, StateSpec}

object TileAggregations {

  sealed trait TrackingEvent
  object TrackingEvent {
    case class VehicleEnteredTile(vehicleId: String, tile: String) extends TrackingEvent
    case class VehicleLeftTile(vehicleId: String, tile: String) extends TrackingEvent
  }

  def toTrackingEvents(input: DStream[StateUpdate]) = {

    def updateState(vehicleId: String, eventOpt: Option[StateUpdate], state: State[String]): Iterator[TrackingEvent] = {
      lazy val currentTile = eventOpt.get.tile
      state.getOption match {
        case Some(registeredTile) =>
          if (state.isTimingOut())
            Iterator(TrackingEvent.VehicleLeftTile(vehicleId, registeredTile))
          else {
            if (registeredTile == currentTile)
              Iterator.empty
            else {
              state.update(currentTile)
              Iterator(
                TrackingEvent.VehicleEnteredTile(vehicleId, currentTile),
                TrackingEvent.VehicleLeftTile(vehicleId, registeredTile)
              )
            }
          }
        case None =>
          state.update(currentTile)
          Iterator(TrackingEvent.VehicleEnteredTile(vehicleId, currentTile))
      }
    }

    input
      .map(su => (su.icao24, su))
      .mapWithState(
        StateSpec
          .function(updateState _)
          .timeout(Minutes(1))
      ).flatMap(identity)
  }

  def updateTileVehicleView(trackingEvents: DStream[TrackingEvent]) = {
    val connector = CassandraConnector(trackingEvents.context.sparkContext)
    trackingEvents.foreachRDD { rdd =>
      rdd.foreachPartition { it =>
        connector.withSessionDo { session =>
          it.foreach {
            case TrackingEvent.VehicleEnteredTile(v, t) =>
              val insertStatement =
                QueryBuilder.insertInto("aired", "tile_vehicle")
                  .value("tile_id", t)
                  .value("vehicle_id", v)
                  .value("tile_id_idx", t)
              session.execute(insertStatement)
            case TrackingEvent.VehicleLeftTile(v, t) =>
              val deleteStatement =
                QueryBuilder.delete().from("aired", "tile_vehicle")
                  .where(QueryBuilder eq ("tile_id", t)).and(QueryBuilder eq ("vehicle_id", v))
              session.execute(deleteStatement)
          }
        }
      }
    }
  }

  def toTileCountUpdates(trackingEvents: DStream[TrackingEvent]): DStream[(String, Int)] = {
    val highestLevel =
      trackingEvents
        .map {
          case TrackingEvent.VehicleEnteredTile(_, t) => (t, 1)
          case TrackingEvent.VehicleLeftTile(_, t) => (t, -1)
        }
    def loop(level: Int, input: DStream[(String, Int)]): DStream[(String, Int)] = {
      val reduced =
        input
          .reduceByKey(
            reduceFunc = _ + _,
            numPartitions = level)
          .filter(_._2 != 0)
      if (level > 1)
        reduced union loop(level - 1, reduced.map { case (t, c) => (t.init, c) })
      else
        reduced
    }
    loop(TileSystem.HighestDetailLevel, highestLevel)
  }

  def updateTileCounts(trackingEvents: DStream[(String, Int)]) = {
    trackingEvents
      .foreachRDD { rdd =>
        rdd.saveToCassandra("aired", "tile", SomeColumns("id" as "_1", "number_of_vehicles" as "_2"))
      }
  }
}
