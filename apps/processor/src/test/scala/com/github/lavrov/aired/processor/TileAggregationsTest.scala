package com.github.lavrov.aired.processor

import com.holdenkarau.spark.testing.StreamingSuiteBase
import TileAggregations.TrackingEvent
import com.github.lavrov.aired.protocol.StateUpdate
import org.scalatest.FunSuite

class TileAggregationsTest extends FunSuite with StreamingSuiteBase {

  test("toTrackingEvents") {
    testOperation[StateUpdate, TrackingEvent](statusUpdates, TileAggregations.toTrackingEvents _, trackingEvents)
  }

  test("toTileCountUpdates") {
    testOperation[TrackingEvent, (String, Int)](trackingEvents, TileAggregations.toTileCountUpdates _, tileCountUpdates)
  }

  def statusUpdates = List(
    StateUpdate(icao24 = "1", timestamp = 0l, longitude = -118.223022, latitude = 33.963379, tile = "02301231131031322012213"),
    StateUpdate(icao24 = "2", timestamp = 0l, longitude = -110.223022, latitude = 32.963379, tile = "02310223102113123202003"),
    StateUpdate(icao24 = "1", timestamp = 0l, longitude = -118.223022, latitude = 33.963379, tile = "02301231131031322012213"),
    StateUpdate(icao24 = "2", timestamp = 0l, longitude = -110.223022, latitude = 33.963379, tile = "02310221120131323002203")
  ) :: Nil

  def trackingEvents = List(
    TrackingEvent.VehicleEnteredTile(vehicleId = "1", tile = "02301231131031322012213"),
    TrackingEvent.VehicleEnteredTile(vehicleId = "2", tile = "02310223102113123202003"),
    TrackingEvent.VehicleLeftTile(vehicleId =    "2", tile = "02310223102113123202003"),
    TrackingEvent.VehicleEnteredTile(vehicleId = "2", tile = "02310221120131323002203")
  ) :: Nil

  def tileCountUpdates = List(
    ("02301231131031322012213", 1),
    ("0230123113103132201221", 1),
    ("023012311310313220122", 1),
    ("02301231131031322012", 1),
    ("0230123113103132201", 1),
    ("023012311310313220", 1),
    ("02301231131031322", 1),
    ("0230123113103132", 1),
    ("023012311310313", 1),
    ("02301231131031", 1),
    ("0230123113103", 1),
    ("023012311310", 1),
    ("02301231131", 1),
    ("0230123113", 1),
    ("023012311", 1),
    ("02301231", 1),
    ("0230123", 1),
    ("023012", 1),
    ("02301", 1),
    ("0230", 1),
    ("023", 2),
    ("02", 2),
    ("0", 2),
    ("02310221120131323002203", 1),
    ("0231022112013132300220", 1),
    ("023102211201313230022", 1),
    ("02310221120131323002", 1),
    ("0231022112013132300", 1),
    ("023102211201313230", 1),
    ("02310221120131323", 1),
    ("0231022112013132", 1),
    ("023102211201313", 1),
    ("02310221120131", 1),
    ("0231022112013", 1),
    ("023102211201", 1),
    ("02310221120", 1),
    ("0231022112", 1),
    ("023102211", 1),
    ("02310221", 1),
    ("0231022", 1),
    ("023102", 1),
    ("02310", 1),
    ("0231", 1)
  ) :: Nil
}
