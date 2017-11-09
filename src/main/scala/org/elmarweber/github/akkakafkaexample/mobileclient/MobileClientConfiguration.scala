package org.elmarweber.github.akkakafkaexample.mobileclient

import com.typesafe.config.ConfigFactory

object MobileClientConfiguration {
  private val rootConfig = ConfigFactory.load()
  val config = rootConfig.getConfig("org.elmarweber.github.akkakafkaexample.mobileclient")


  case class GatewayConfig(endpoint: String)

  val gateway = GatewayConfig(config.getString("gateway.endpoint"))
}
