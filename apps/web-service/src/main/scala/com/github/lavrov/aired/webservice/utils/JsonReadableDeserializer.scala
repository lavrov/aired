package com.github.lavrov.aired.webservice.utils

import org.apache.kafka.common.serialization.Deserializer
import play.api.libs.json.{Json, Reads}

class JsonReadableDeserializer[T: Reads] extends Deserializer[T] {

  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]): T = Json.parse(data).as[T]
}
