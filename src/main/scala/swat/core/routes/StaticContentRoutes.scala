package swat.core.routes

import cats.effect.{Blocker, ContextShift, IO}
import org.http4s.dsl.io._
import org.http4s.rho.RhoRoutes
import org.http4s.{HttpRoutes, Request, Response, StaticFile}

object StaticContentRoutes {
  private val swaggerUiDir = "/swagger-ui"

  def routes(blocker: Blocker)(implicit cs: ContextShift[IO]): HttpRoutes[IO] = new RhoRoutes[IO] {
    GET / "css" / * |>> { (req: Request[IO], _: List[String]) => fetchResource(swaggerUiDir + req.pathInfo, req, blocker) }
    GET / "images" / * |>> { (req: Request[IO], _: List[String]) => fetchResource(swaggerUiDir + req.pathInfo, req, blocker) }
    GET / "lib" / * |>> { (req: Request[IO], _: List[String]) => fetchResource(swaggerUiDir + req.pathInfo, req, blocker) }
    GET / "swagger-ui" |>> { req: Request[IO] => fetchResource(swaggerUiDir + "/index.html", req, blocker) }
    GET / "swagger-ui.js" |>> { req: Request[IO] => fetchResource(swaggerUiDir + "/swagger-ui.min.js", req, blocker) }
  }.toRoutes()

  private def fetchResource(path: String, req: Request[IO], blocker: Blocker)(implicit cs: ContextShift[IO]): IO[Response[IO]] =
    StaticFile.fromResource(path, blocker, Some(req)).getOrElseF(NotFound())
}
