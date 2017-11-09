package org.elmarweber.github.akkakafkaexample.statistics

import com.typesafe.config.ConfigFactory

object StatisticsConfiguration {
  private val rootConfig = ConfigFactory.load()

  val config = rootConfig.getConfig("org.elmarweber.github.akkakafkaexample.statistics")


  case class KafkaConfig(bootstrapServers: String, sourceTopic: String, targetTopic: String, clientId: String, groupId: String)

  val kafka = KafkaConfig(
    bootstrapServers = config.getString("kafka.bootstrap-servers"),
    sourceTopic = config.getString("kafka.source-topic"),
    targetTopic = config.getString("kafka.target-topic"),
    clientId = config.getString("kafka.client-id"),
    groupId = config.getString("kafka.group-id"))
}
