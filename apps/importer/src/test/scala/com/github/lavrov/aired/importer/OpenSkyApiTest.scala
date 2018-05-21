package com.github.lavrov.aired.importer

import com.github.lavrov.aired.importer.OpenSkyApi.{Response, StateUpdate}
import org.scalatest.Matchers._
import play.api.libs.json.{JsSuccess, Json}

class OpenSkyApiTest extends org.scalatest.FunSuite {

  test("responseReader") {
    val rawMessage =
      """{
        |  "time":1519421750,
        |  "states": [[
        |    "7c35e8",
        |    "KXM     ",
        |    "Australia",
        |    1519421750,
        |    1519421750,
        |    147.2281,
        |    -22.8316,
        |    12192,
        |    false,
        |    162.04,
        |    310.36,
        |    0,
        |    null,
        |    12900.66,
        |    "4762",
        |    false,
        |    0
        |  ]]
        |}""".stripMargin
    val message = OpenSkyApi.responseReader.reads(Json parse rawMessage)
    message shouldEqual JsSuccess(Response(1519421750, StateUpdate("7c35e8", 147.2281, -22.8316) :: Nil))
  }

}
