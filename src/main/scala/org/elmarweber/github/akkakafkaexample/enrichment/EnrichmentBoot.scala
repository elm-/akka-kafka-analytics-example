package org.elmarweber.github.akkakafkaexample.enrichment

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import kamon.Kamon
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer, StringSerializer}
import org.elmarweber.github.akkakafkaexample.eventgateway.EventGatewayBoot.{eventRoute, logger}
import org.elmarweber.github.akkakafkaexample.eventgateway.{EventGatewayConfiguration, EventsToKafkaStream}

object EnrichmentBoot extends App with StrictLogging {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  Kamon.start()

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(EnrichmentConfiguration.kafka.bootstrapServers)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    .withProperty(ConsumerConfig.CLIENT_ID_CONFIG, s"${EnrichmentConfiguration.kafka.clientId}")
    .withProperty(ConsumerConfig.GROUP_ID_CONFIG, s"${EnrichmentConfiguration.kafka.clientId}.${EnrichmentConfiguration.kafka.groupId}")

  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(EnrichmentConfiguration.kafka.bootstrapServers)

  val streamFuture = EnrichmentStream.createStream(consumerSettings, producerSettings,
    EnrichmentConfiguration.kafka.sourceTopic, EnrichmentConfiguration.kafka.targetTopic)


  streamFuture.onFailure { case ex =>
    logger.error(s"Stream filed unexpectedly: ${ex.getMessage}", ex)
    sys.exit(1)
  }
}
