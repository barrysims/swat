package swat.core.routes

import cats.effect.IO
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._
import org.http4s.{Query, Uri}

/**
  * Provides a redirect to /static/swagger-ui with a url parameter that points to the JSON swagger
  */
class SwaggerRoutes(val path: String) extends RhoRoutes[IO] {

  "swagger ui" ** GET |>>
    TemporaryRedirect(Uri(path = "/static/swagger-ui", query = Query("url" -> Some(s"$path/swagger.json"))))

}