package com.github.lavrov.aired.protocol

import play.api.libs.json.{Json, OFormat}

case class StateUpdate(
    icao24: String,
    timestamp: Long,
    longitude: Double,
    latitude: Double,
    tile: String
)

object StateUpdate {
  implicit val Formatter: OFormat[StateUpdate] = Json.format[StateUpdate]
}
