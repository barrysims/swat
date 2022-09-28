package swat.examples

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.headers.Authorization
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.models._
import org.http4s.rho.swagger.syntax.io._
import org.http4s.server.jetty.JettyBuilder
import org.http4s.server.middleware.{GZip, Logger}
import swat.core.middleware.{Custom404, TraceToken}
import swat.core.routes.{StaticContentRoutes, SwaggerRoutes}
import swat.examples.contextexamples.ContextExamplesRoutes
import swat.examples.todo.TodoRoutes
import swat.examples.helloworld.HelloWorldRoutes

import scala.concurrent.duration._
import scala.language.postfixOps


object Main extends IOApp {

  val v1 = "/api/v1"

  def run(args: List[String]): IO[ExitCode] = Blocker[IO].use { blocker =>

    // Swagger is served from api/v1/swagger.json
    // Swagger-UI is served from api/v1/swagger

    val apiV1 = createRhoMiddleware(
      apiInfo = Info("Example API", "v1"),
      basePath = Some(v1),
      securityDefinitions = Map(
        "api-key" -> ApiKeyAuthDefinition(
          name = "x-api-key",
          in = In.HEADER
        ),
        "authorization" -> ApiKeyAuthDefinition(
          name = Authorization.name.value,
          in = In.HEADER
        )
      ),
      security = List(
        SecurityRequirement(name = "api-key", scopes = List.empty),
        SecurityRequirement(name = "authorization", scopes = List.empty)
      )
    )

    // These are triggered in the order they appear
    val commonMiddleware: HttpRoutes[IO] => HttpRoutes[IO] =
      TraceToken.withTraceToken compose
        Custom404.withCustomNotFoundResponse compose
        Logger.httpRoutes[IO](logHeaders = false, logBody = false)

    val routes = {
      commonMiddleware
    } compose {
      new HelloWorldRoutes and
      new TodoRoutes and
      new ContextExamplesRoutes
    }.toRoutes

    JettyBuilder[IO]
      .mountService(GZip(routes(apiV1)), prefix = v1)
      .mountService(commonMiddleware(StaticContentRoutes.routes(blocker)), prefix = "/static")
      .mountService(commonMiddleware(new SwaggerRoutes(v1).toRoutes()), prefix = s"$v1/swagger")
      .mountService(commonMiddleware(new RhoRoutes[IO].toRoutes()), prefix = "/")
      .bindHttp(5000)
      .withIdleTimeout(5 minutes)
      .withAsyncTimeout(5 minutes)
      .withoutBanner
      .serve.compile.drain.as(ExitCode.Success)
  }
}
