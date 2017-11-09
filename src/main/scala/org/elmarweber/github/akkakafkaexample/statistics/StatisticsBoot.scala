package org.elmarweber.github.akkakafkaexample.statistics

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer, StringSerializer}
import org.elmarweber.github.akkakafkaexample.eventgateway.EventGatewayBoot.{eventRoute, logger}
import org.elmarweber.github.akkakafkaexample.eventgateway.{EventGatewayConfiguration, EventsToKafkaStream}

object StatisticsBoot extends App with StrictLogging {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(StatisticsConfiguration.kafka.bootstrapServers)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    .withProperty(ConsumerConfig.CLIENT_ID_CONFIG, s"${StatisticsConfiguration.kafka.clientId}")
    .withProperty(ConsumerConfig.GROUP_ID_CONFIG, s"${StatisticsConfiguration.kafka.clientId}.${StatisticsConfiguration.kafka.groupId}")

  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(EventGatewayConfiguration.kafka.bootstrapServers)

  val streamFuture = StatisticsStream.createStream(consumerSettings, producerSettings,
    StatisticsConfiguration.kafka.sourceTopic, StatisticsConfiguration.kafka.targetTopic)


  streamFuture.onFailure { case ex =>
    logger.error(s"Stream filed unexpectedly: ${ex.getMessage}", ex)
    sys.exit(1)
  }
}
