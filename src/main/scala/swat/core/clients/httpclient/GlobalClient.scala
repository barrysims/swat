package swat.core.clients.httpclient

import java.util.concurrent.ForkJoinPool

import cats.effect.{ContextShift, IO, Resource, Timer}
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.http4s.client.Client
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.client.dsl.Http4sClientDsl

import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object GlobalClient extends Http4sClientDsl[IO] {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  // Global AsyncHttpClient for all clients.
  private implicit val clientExecutionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool())

  // Configure custom timeouts to cope with long response times for some Salesforce requests.
  private val configBuilder = new DefaultAsyncHttpClientConfig.Builder()
    .setConnectTimeout(10 * 1000)
    .setRequestTimeout(5 * 60 * 1000)
    .setReadTimeout(5 * 60 * 1000)
    .setMaxRequestRetry(1)

  private def clientResourceForConf(cb: DefaultAsyncHttpClientConfig.Builder) = AsyncHttpClient.resource[IO](cb.build())

  val client: Resource[IO, Client[IO]] = clientResourceForConf(configBuilder)
}
