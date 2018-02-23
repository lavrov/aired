package com.github.lavrov.aired.maps

object TileSystem {

  val HighestDetailLevel = 23

  def fromTileXY(x: Int, y: Int, zoomLevel: Int): String = {
    val quadkeyChars = new Array[Char](zoomLevel)
    var i = zoomLevel
    while (i > 0) {
      var digit = '0'
      val mask = 1 << (i - 1)
      if ((x & mask) != 0) digit = (digit + 1).toChar
      if ((y & mask) != 0) digit = (digit + 2).toChar
      quadkeyChars(zoomLevel - i) = digit
      i -= 1
    }
    new String(quadkeyChars)
  }

  def fromLonLat(lon: Double, lat: Double, zoomLevel: Int): String = {
    var xTile = Math.floor((lon + 180) / 360 * (1 << zoomLevel)).toInt
    var yTile = Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoomLevel)).toInt
    if (xTile < 0) xTile = 0
    if (xTile >= (1 << zoomLevel)) xTile = (1 << zoomLevel) - 1
    if (yTile < 0) yTile = 0
    if (yTile >= (1 << zoomLevel)) yTile = (1 << zoomLevel) - 1
    fromTileXY(xTile, yTile, zoomLevel)
  }

  def parentFor(tile: String): Option[String] = if (tile.length > 1) Some(tile.init) else None

  def rootPathFor(tile: String): List[String] =
    parentFor(tile) match {
      case Some(parent) => parent :: rootPathFor(parent)
      case None => Nil
    }

  def isLeaf(tile: String) = tile.length == HighestDetailLevel
}
