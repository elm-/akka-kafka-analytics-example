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
    val (queue, future) = Source.queue[AnalyticsEvent](1024, OverflowStrategy.backpressure)
      .map { event =>
        logger.debug(s"Received event ${event.id} from ${event.clientId}")
        event
      }
      .map { event =>
        ProducerMessage.Message(new ProducerRecord[String, String](topic, event.id, event.toJson.toString), NotUsed)
      }
      .via(Producer.flow(producerSettings))
      .toMat(Sink.ignore)(Keep.both)
      .run()
    (queue, future)
  }
}

object EventsToKafkaStream extends EventsToKafkaStream
