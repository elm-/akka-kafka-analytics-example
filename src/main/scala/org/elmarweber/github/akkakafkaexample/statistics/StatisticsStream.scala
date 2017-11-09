package org.elmarweber.github.akkakafkaexample.statistics

import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.elmarweber.github.akkakafkaexample.lib.{AnalyticsEvent, EnrichedAnalyticsEvent}
import spray.json._

import scala.concurrent.duration._

trait StatisticsStream extends StrictLogging {
  def createStream(consumerSettings: ConsumerSettings[String, String],
                   producerSettings: ProducerSettings[String, String],
                   sourceTopic: String, targetTopic: String)(implicit fmt: Materializer) = {
    // aggregate per artist each 10 seconds how many plays it has
  }
}


object StatisticsStream extends StatisticsStream