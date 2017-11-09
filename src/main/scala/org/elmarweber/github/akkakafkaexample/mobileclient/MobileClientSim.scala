package org.elmarweber.github.akkakafkaexample.mobileclient

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding
import akka.stream.ActorMaterializer
import org.elmarweber.github.akkakafkaexample.lib.{AnalyticsEvent, HttpClient}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
 * Generates some random events and sends them to the server.
 */
object MobileClientSim extends App {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val gatewayClient = HttpClient.fromEndpoint(MobileClientConfiguration.gateway.endpoint).build()


  while (true) {
    val event = createEvent()
    val request = RequestBuilding.Post("/api/events", event)
    Await.result(gatewayClient.doCheckedRest(request), 1.second)
    Thread.sleep(50)
  }

  def createEvent() = {
    AnalyticsEvent(
      id = UUID.randomUUID().toString(),
      clientId = Random.shuffle(ClientIds).head,
      songTitle = "test",
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
