package org.elmarweber.github.akkakafkaexample.enrichment

import java.net.InetAddress

import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging
import kamon.Kamon
import org.apache.kafka.clients.producer.ProducerRecord
import org.elmarweber.github.akkakafkaexample.lib.{AnalyticsEvent, EnrichedAnalyticsEvent}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait EnrichmentStream extends StrictLogging {
  def createStream(consumerSettings: ConsumerSettings[String, String],
                   producerSettings: ProducerSettings[String, String],
                   sourceTopic: String, targetTopic: String)
                  (implicit fmt: Materializer, ec: ExecutionContext) = {
    // extract the artist name from the song title, commit each message
  }
}


object EnrichmentStream extends EnrichmentStream