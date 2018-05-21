package com.github.lavrov.aired.maps

import org.scalatest.FunSuite
import org.scalatest.Matchers._

class TileSystemTest extends FunSuite {

  test("fromTileXY") {
    TileSystem.fromTileXY(3, 5, 3) shouldEqual "213"
  }

  test("fromLonLat") {
    TileSystem.fromLonLat(4.48080, 51.91373, 18) shouldEqual "120202112030311310"
  }

}
