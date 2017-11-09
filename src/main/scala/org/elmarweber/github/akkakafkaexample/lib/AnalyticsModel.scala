package org.elmarweber.github.akkakafkaexample.lib

import spray.json._

sealed trait AnalyticsModel

object AnalyticsModel extends DefaultJsonProtocol {
  implicit val AnalyticsEventFormat = jsonFormat4(AnalyticsEvent)
}

case class AnalyticsEvent(
  id: String,
  clientId: String,
  songTitle: String,
  ip: String
) extends AnalyticsModel
