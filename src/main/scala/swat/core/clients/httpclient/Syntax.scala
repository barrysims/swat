package swat.core.clients.httpclient

import org.http4s.{Header, Headers, Uri}
import swat.core.syntax.ApiHeader
import swat.core.syntax.conf.Conf

object Syntax {

  implicit class StringOps(val s: String) extends AnyVal {
    // Fails with throwable, so only use in initialising code
    def / (right: String): Uri = Uri.unsafeFromString(s"$s/$right")
  }

  implicit class UriOps(val uri: Uri) extends AnyVal {
    def / (path: Int): Uri = uri / path.toString
  }

  implicit class HeadersOps(h: Headers) {
    def withTraceToken(c: Conf) = h.put(Header(ApiHeader.TraceToken, c.traceToken))
  }

}
