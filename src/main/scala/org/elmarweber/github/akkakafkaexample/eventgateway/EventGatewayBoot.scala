package org.elmarweber.github.akkakafkaexample.eventgateway

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

object EventGatewayBoot extends App with EventRoute with StrictLogging {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(EventGatewayConfiguration.kafka.bootstrapServers)

  val (queue, streamFuture) = EventsToKafkaStream.createStream(producerSettings, EventGatewayConfiguration.kafka.targetTopic)

  Http().bindAndHandle(eventRoute(queue), "0.0.0.0", 9090).transform(
    binding => logger.info(s"REST interface bound to ${binding.localAddress} "),
    { t => logger.error(s"Couldn't bind interface: ${t.getMessage}", t); sys.exit(1) }
  )

  streamFuture.onFailure { case ex =>
    logger.error(s"Stream filed unexpectedly: ${ex.getMessage}", ex)
    sys.exit(1)
  }
}
