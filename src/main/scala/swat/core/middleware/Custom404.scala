package swat.core.middleware

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.syntax.StringSyntax
import org.http4s.{Headers, HttpRoutes, Request, Response, Status}
import swat.core.syntax.conf.Conf
import swat.core.syntax.{ApiHeader, Ctxt}

object Custom404 extends Ctxt[Conf] with StringSyntax {

  val withCustomNotFoundResponse: HttpRoutes[IO] => HttpRoutes[IO] = service => Kleisli { request =>
    service(request).orElseF(request.toNotFoundResponse)
  }

  private implicit class NotFoundRequestOps(val r: Request[IO]) extends AnyVal {
    def toNotFoundResponse: IO[Option[Response[IO]]] = IO(Some(Response(
      status = Status.NotFound,
      headers = Headers(r.headers.find(_.name == ApiHeader.TraceToken.ci).toList)
    )))
  }
}
