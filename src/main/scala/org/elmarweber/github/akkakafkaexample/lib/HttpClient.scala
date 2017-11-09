package org.elmarweber.github.akkakafkaexample.lib

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.model.Uri._
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl._
import org.asynchttpclient.util.StringUtils
import org.elmarweber.github.akkakafkaexample.lib.HttpClient.{HttpScheme, HttpsScheme, Scheme}
import org.slf4s.Logging
import spray.json._

import scala.concurrent._
import scala.util._

trait HttpClientPreProcessor {
  def process(request: HttpRequest): Future[HttpRequest]
}

trait SyncHttpClientPreProcessor extends HttpClientPreProcessor {
  override def process(request: HttpRequest): Future[HttpRequest] = Future.successful(processSync(request))
  def processSync(request: HttpRequest): HttpRequest
}

case class AddHeaderProc(header: HttpHeader) extends SyncHttpClientPreProcessor {
  override def processSync(request: HttpRequest) = request.addHeader(header)
}

trait HttpClientPostProcessor {
  def process(request: HttpRequest, response: Try[HttpResponse]): Future[Try[HttpResponse]]
}

trait SyncHttpClientPostProcessor extends HttpClientPostProcessor {
  override def process(request: HttpRequest, response: Try[HttpResponse]): Future[Try[HttpResponse]] = Future.successful(processSync(request, response))
  def processSync(request: HttpRequest, response: Try[HttpResponse]): Try[HttpResponse]
}

object RequestResponseLoggingPostProcessor extends SyncHttpClientPostProcessor with Logging {
  override def processSync(request: HttpRequest, response: Try[HttpResponse]) = {
    log.debug(s"Send Request ${request.toString}")
    response.foreach { resp =>
      log.debug(s"Received response Request ${resp.toString}")
    }
    response
  }
}


object RequestUrlLoggingPreProcessor extends SyncHttpClientPreProcessor with Logging {
  private val counter = new AtomicLong(1)

  override def processSync(request: HttpRequest) = {
    log.info(s"Send Request ${counter.getAndIncrement()} ${request.method.value} ${request.uri}")
    request
  }
}

object RequestUrlLoggingPostProcessor extends SyncHttpClientPostProcessor with Logging {
  private val counter = new AtomicLong(1)

  override def processSync(request: HttpRequest, response: Try[HttpResponse]) = {
    val counterValue = counter.getAndIncrement()
    log.info(s"Send Request ${counterValue} ${request.method.value} ${request.uri}")
    response.foreach { resp =>
      log.info(s"Received Response ${counterValue} ${request.method.value} ${response.map(_.status.value)} ${request.uri}")
    }
    response
  }
}


class HttpClient(
    host: String,
    port: Int,
    scheme: Scheme = HttpScheme,
    defaultPrefix: Option[String] = None,
    settings: Option[ConnectionPoolSettings] = None,
    preProcs: Seq[HttpClientPreProcessor] = Nil,
    postProcs: Seq[HttpClientPostProcessor] = Nil
  )(implicit system: ActorSystem, ec: ExecutionContext, fm: Materializer) {

  private val cleanPrefix = defaultPrefix.map { prefix =>
    var clean = prefix
    if (!clean.startsWith("/"))
      clean = "/" + clean
    if (clean.endsWith("/"))
      clean = clean.substring(0, clean.size - "/".size)
    clean
  }
  private val pool = scheme match {
    case HttpScheme => Http(system).cachedHostConnectionPool[HttpRequest](host, port, settings = settings.getOrElse(ConnectionPoolSettings(system)))
    case HttpsScheme => Http(system).cachedHostConnectionPoolHttps[HttpRequest](host, port, settings = settings.getOrElse(ConnectionPoolSettings(system)))
  }


  def doRest(req: HttpRequest): Future[HttpResponse] = {
    val fixedUri = cleanPrefix match {
      case None => req.uri
      case Some(prefix) => req.uri.withPath(Path(prefix) ++ req.uri.path)
    }
    val fixedRequest = req
      .withUri(fixedUri)
    val source = Source.single(fixedRequest -> req)
    val preProcessed = preProcs.foldLeft(source) { case (stage, proc) =>
      stage.mapAsync(parallelism = 1) { case (req, i) => proc.process(req).map(r => r -> r)}
    }
    val poolFlow = preProcessed.via(pool)
    val postProcessed = postProcs.foldLeft(poolFlow) { case (stage, proc) =>
      stage.mapAsync(parallelism = 1) { case (resp, req) => proc.process(req, resp).map(r => r -> req) }
    }
    postProcessed.runWith(Sink.head).flatMap {
      case (Success(response), _) => Future.successful(response)
      case (Failure(ex), _) => Future.failed(ex)
    }
  }

  def doCheckedRest(req: HttpRequest): Future[HttpResponse] = {
    doRest(req).flatMap { response =>
      if (response.status.isSuccess()) {
        Future.successful(response)
      } else {
        val errorMessage = s"Could not complete rest call and did not expect failed call to ${req.uri} with status ${response.status} ${response.status.intValue}.\n" +
            s"debug details follow for request to ${host}:${port}/${req.uri}\n" +
            s"${req.headers.toString}\n" +
            s"${req.entity.toString}" +
            s"debug details for response\n" +
            s"${response.headers.toString}\n" +
            s"${response.entity.toString}\n"
        Future.failed(new IllegalStateException(errorMessage))
      }
    }
  }

  def doTypedRest[T : JsonFormat](req: HttpRequest): Future[T] = {
    doCheckedRest(req).flatMap { response =>
      Unmarshal(response.entity).to[String].flatMap { data =>
        Try(data.parseJson.convertTo[T]) match {
          case Success(t) => Future.successful(t)
          case Failure(ex) =>
            val errorMessage = s"Could not complete rest call due to JSON parsing exception for ${req.uri} on data ${data}"
            Future.failed(new IllegalArgumentException(errorMessage, ex))
        }
      }
    }
  }

  def doTypedRestEither[T : JsonFormat](req: HttpRequest): Future[Either[HttpResponse, T]] = {
    doRest(req).flatMap {
      case response if response.status.isSuccess() =>
        Unmarshal(response.entity).to[String].flatMap { data =>
          Try(data.parseJson.convertTo[T]) match {
            case Success(t) => Future.successful(Right(t))
            case Failure(ex) =>
              val errorMessage = s"Could not complete rest call due to JSON parsing exception for ${req.uri} on data ${data}"
              Future.failed(new IllegalArgumentException(errorMessage, ex))
          }
        }
      case response => Future.successful(Left(response))
    }
  }


  def doTypedRestWithOriginalJson[T : JsonFormat](req: HttpRequest): Future[(T, JsValue)] = {
    doCheckedRest(req).flatMap { response =>
      Unmarshal(response.entity).to[String].map { c => val j = c.parseJson ; (j.convertTo[T], j) }
    }
  }
}

object HttpClient {
  sealed trait Scheme
  case object HttpScheme extends Scheme
  case object HttpsScheme extends Scheme

  protected def settingsFromUri(uri: Uri) = {
    val scheme = uri.scheme match {
      case "http" => HttpScheme
      case "https" => HttpsScheme
    }
    val host = uri.authority.host.address
    val port = uri.effectivePort
    val prefix = uri.path.toString match {
      case "" => None
      case str => Some(str)
    }
    (scheme, host, port, prefix)
  }

  case class Builder(
    host: String,
    port: Int,
    scheme: Scheme = HttpScheme,
    defaultPrefix: Option[String] = None,
    settings: Option[ConnectionPoolSettings] = None,
    preProcs: Seq[HttpClientPreProcessor] = Nil,
    postProcs: Seq[HttpClientPostProcessor] = Nil
  ) {
    def build()(implicit system: ActorSystem, ec: ExecutionContext, fm: Materializer) =
      new HttpClient(host, port, scheme, defaultPrefix, settings, preProcs, postProcs)

    def withHost(host: String) = copy(host = host)

    def withPort(port: Int) = copy(port = port)

    def withScheme(scheme: Scheme) = copy(scheme = scheme)

    def withDefaultPrefix(defaultPrefix: Option[String]) = copy(defaultPrefix = defaultPrefix)

    def addPreProc(proc: HttpClientPreProcessor) = copy(preProcs = preProcs ++ Seq(proc))

    def addPostProc(proc: HttpClientPostProcessor) = copy(postProcs = postProcs ++ Seq(proc))

    def addHeader(header: HttpHeader) = addPreProc(AddHeaderProc(header))
  }

  def fromEndpoint(endpoint: Uri) = {
    val (scheme, host, port, prefix) = settingsFromUri(endpoint)
    Builder(host, port, scheme, prefix)
  }
}