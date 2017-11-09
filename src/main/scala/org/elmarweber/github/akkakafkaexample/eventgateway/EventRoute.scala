package org.elmarweber.github.akkakafkaexample.eventgateway

import akka.http.scaladsl.server._
import org.elmarweber.github.akkakafkaexample.lib.AnalyticsEvent
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.scaladsl.SourceQueueWithComplete

trait EventRoute extends Directives {
  def eventRoute(queue: SourceQueueWithComplete[AnalyticsEvent]) = pathPrefix("api") {
    pathPrefix("event") {
      post {
        entity(as[AnalyticsEvent]) { event =>
          complete {
            queue.offer(event)
            "OK 1"
          }
        } ~
        entity(as[List[AnalyticsEvent]]) { events =>
          complete {
            events.foreach(queue.offer)
            s"OK ${events.size}"
          }
        }
      }
    }
  }
}
