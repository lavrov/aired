package com.github.lavrov.aired.importer

import play.api.libs.json.{Json, Reads, __}
import play.api.libs.functional.syntax._

object OpenSkyApi {

  case class Response(
      time: Long,
      states: List[StateUpdate]
  )

  case class StateUpdate(
    icao24: String,
    longitude: Double,
    latitude: Double
  )

  implicit val stateUpdateReader: Reads[StateUpdate]= (
    (__ \ 0).read[String] and
    (__ \ 5).read[Double] and
    (__ \ 6).read[Double]
  )(StateUpdate.apply _)

  implicit val responseReader: Reads[Response] = Json.reads[Response]

}
