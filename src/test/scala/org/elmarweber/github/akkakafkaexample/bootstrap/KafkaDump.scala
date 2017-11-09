package org.elmarweber.github.akkakafkaexample.bootstrap

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.elmarweber.github.akkakafkaexample.enrichment.EnrichmentConfiguration

import scala.util.{Failure, Success}

object KafkaDump extends App {
  val DefaultTopic = "events.input"
  //val DefaultTopic = "events.enriched"
  //val DefaultTopic = "artist.stats"
  val NumberOfMessagesToDump = 10

  val topicToDump = if (args.isDefinedAt(0)) args(0) else DefaultTopic

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    .withProperty(ConsumerConfig.CLIENT_ID_CONFIG, "test-client")
    .withProperty(ConsumerConfig.GROUP_ID_CONFIG, "test-group")

  val future = Consumer.plainSource(consumerSettings, Subscriptions.topics(topicToDump))
    .take(NumberOfMessagesToDump)
    .runWith(Sink.foreach { record =>
      println(record.key() + ": " + record.value())
    })

  future.onComplete {
    case Success(_) =>
      System.exit(0)
    case Failure(ex) =>
      ex.printStackTrace()
      System.exit(1)
  }
}
