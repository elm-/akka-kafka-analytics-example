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
    Consumer.committableSource(consumerSettings, Subscriptions.topics(sourceTopic))
      .map { msg =>
        (msg.record.value().parseJson.convertTo[EnrichedAnalyticsEvent], msg.committableOffset)
      }
      .groupBy(1024, { case (event, _) => event.artist })
      .groupedWithin(Integer.MAX_VALUE, 10.seconds)
      .map { groupedData =>
        val events = groupedData.map(_._1)
        val offsets = groupedData.map(_._2)
        val batchOffset = offsets.foldLeft(CommittableOffsetBatch.empty) { case (batch, offset) => batch.updated(offset) }
        val playCount = events.size
        val windowEnd = System.currentTimeMillis()
        val key = s"${events.head.artist}_${windowEnd}"
        logger.info(s"Aggregated for artist '${events.head.artist}' ${playCount} play events")
        ProducerMessage.Message(new ProducerRecord(targetTopic, key, playCount.toString), batchOffset)
      }
      .mergeSubstreams
      .via(Producer.flow(producerSettings))
      .groupedWithin(1024, 5.seconds)
      .mapAsync(1) { producerMessages =>
        val latestBatchOffset = producerMessages.last.message.passThrough
        latestBatchOffset.commitScaladsl()
      }
      .runWith(Sink.ignore)
  }
}


object StatisticsStream extends StatisticsStream