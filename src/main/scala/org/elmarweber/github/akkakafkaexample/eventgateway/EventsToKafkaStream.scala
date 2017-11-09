package org.elmarweber.github.akkakafkaexample.eventgateway

import akka.NotUsed
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl._
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.elmarweber.github.akkakafkaexample.lib.AnalyticsEvent
import spray.json._

trait EventsToKafkaStream extends StrictLogging {
  def createStream(producerSettings: ProducerSettings[String, String], topic: String)(implicit fmt: Materializer) = {
    // create a queue and push data to kafka topic
  }
}

object EventsToKafkaStream extends EventsToKafkaStream
