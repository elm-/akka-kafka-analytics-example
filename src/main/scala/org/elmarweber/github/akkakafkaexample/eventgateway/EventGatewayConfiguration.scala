package org.elmarweber.github.akkakafkaexample.eventgateway

import com.typesafe.config.ConfigFactory

object EventGatewayConfiguration {
  private val rootConfig = ConfigFactory.load()

  val config = rootConfig.getConfig("org.elmarweber.github.akkakafkaexample.eventgateway")


  case class KafkaConfig(bootstrapServers: String, targetTopic: String, clientId: String, groupId: String)

  val kafka = KafkaConfig(
     bootstrapServers = config.getString("kafka.bootstrap-servers"),
     targetTopic = config.getString("kafka.target-topic"),
     clientId = config.getString("kafka.client-id"),
     groupId = config.getString("kafka.group-id"))
}
