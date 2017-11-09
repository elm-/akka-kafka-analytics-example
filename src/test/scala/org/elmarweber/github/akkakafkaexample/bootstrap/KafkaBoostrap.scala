package org.elmarweber.github.akkakafkaexample.bootstrap

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}


object KafkaBoostrap extends App {
  implicit val embeddedKafkaConfig = EmbeddedKafkaConfig(9092, 2182)

  EmbeddedKafka.start()(embeddedKafkaConfig)
}
