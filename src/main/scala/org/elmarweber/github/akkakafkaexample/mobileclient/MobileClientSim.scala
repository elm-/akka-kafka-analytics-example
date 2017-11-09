package org.elmarweber.github.akkakafkaexample.mobileclient

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding
import akka.stream.{ActorMaterializer, ThrottleMode}
import org.elmarweber.github.akkakafkaexample.lib.{AnalyticsEvent, HttpClient}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.duration._
import scala.util.Random
import akka.stream.scaladsl._
import scala.language.postfixOps

/**
 * Generates some random events and sends them to the server.
 */
object MobileClientSim extends App {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val gatewayClient = HttpClient.fromEndpoint(MobileClientConfiguration.gateway.endpoint).build()

  val songs = List("test", "test2", "test3")
  val rand = Random

  RestartSource.withBackoff(1 second, 5 seconds, .5d){ () =>
    Source
      .repeat(createEvent())
      .mapAsyncUnordered(1){ event =>
        val request = RequestBuilding.Post("/api/events", event)
        gatewayClient.doCheckedRest(request)
      }
      .throttle(1, 50 millis, 1, ThrottleMode.Shaping)
  }.runWith(Sink.ignore)


  def createEvent() = {
    AnalyticsEvent(
      id = UUID.randomUUID().toString(),
      clientId = Random.shuffle(ClientIds).head,
      songTitle = songs(rand.nextInt(songs.size)),
      ip = "127.0.0.1"
    )
  }

  def ClientIds = List(
    "elmar",
    "john",
    "jack",
    "mark"
  )
}
