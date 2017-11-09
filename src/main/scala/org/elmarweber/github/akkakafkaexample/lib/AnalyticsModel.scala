package org.elmarweber.github.akkakafkaexample.lib

import spray.json._

sealed trait AnalyticsModel

object AnalyticsModel extends DefaultJsonProtocol {
  implicit val AnalyticsEventFormat = jsonFormat4(AnalyticsEvent)
  implicit val EnrichedAnalyticsEventFormat = jsonFormat5(EnrichedAnalyticsEvent)
}

case class AnalyticsEvent(
  id: String,
  clientId: String,
  songTitle: String,
  ip: String
) extends AnalyticsModel


case class EnrichedAnalyticsEvent(
  id: String,
  clientId: String,
  songTitle: String,
  artist: String,
  ip: String
) extends AnalyticsModel
