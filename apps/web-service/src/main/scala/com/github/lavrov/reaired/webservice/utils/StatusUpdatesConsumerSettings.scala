package com.github.lavrov.aired.webservice.utils

import akka.kafka.ConsumerSettings
import com.github.lavrov.aired.protocol.StateUpdate

case class StatusUpdatesConsumerSettings(
    settings: ConsumerSettings[String, StateUpdate]
)
