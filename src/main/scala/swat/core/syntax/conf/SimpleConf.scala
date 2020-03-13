package swat.core.syntax.conf

import java.util.UUID

import cats.effect.IO
import org.http4s.Request
import org.http4s.syntax.StringSyntax
import swat.core.syntax.ApiHeader


case class SimpleConf(
  traceToken: String,
) extends Conf

object SimpleConf extends StringSyntax {
  def fromRequest(r: Request[IO]): SimpleConf = {
    val traceToken = r.headers.find(_.name == ApiHeader.TraceToken.ci).map(_.value)

    SimpleConf(
      traceToken = traceToken.getOrElse(UUID.randomUUID().toString)
    )
  }
}