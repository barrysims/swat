package swat.core.middleware

import java.util.UUID

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.{Header, HttpRoutes}
import swat.core.syntax.ApiHeader

object TraceToken {

  /**
    * Add a generated trace-token header to the request if none was present.
    */
  val withTraceToken: HttpRoutes[IO] => HttpRoutes[IO] = service => Kleisli { request =>
    service(
      request.headers.find(_.name == ApiHeader.TraceToken) match {
        case None => request.withHeaders(request.headers.put(Header(ApiHeader.TraceToken, UUID.randomUUID().toString)))
        case _ => request
      }
    )
  }
}
