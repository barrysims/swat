package swat.core.syntax.conf

import cats.effect.IO
import doobie.util.transactor.Transactor.Aux
import org.http4s.{Header, Headers}
import swat.core.syntax._

trait Conf {
  val traceToken: String
  def responseHeaders: Headers = Headers(List(Header(ApiHeader.TraceToken, traceToken)))
}

trait HasTransactor {
  val transactor: Aux[IO, Unit]
}