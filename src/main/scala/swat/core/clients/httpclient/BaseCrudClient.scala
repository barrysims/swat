package swat.core.clients.httpclient

import cats.effect.{IO, Resource}
import org.http4s.Method.{DELETE, GET, PATCH, POST, PUT, PermitsBody}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{EntityDecoder, Headers, Method, Query, Request, Response, Status, Uri}
import swat.core.syntax.Ctxt
import swat.core.syntax.Error._
import swat.core.syntax.conf.SimpleConf
import Syntax._
import org.http4s.client.Client


trait BaseHttpClient extends Http4sClientDsl[IO] with Ctxt[SimpleConf] {

  protected val client: Resource[IO, Client[IO]] = GlobalClient.client

  protected def doRequestEmptyBody[A](method: Method, uri: Uri, params: Seq[(String, Option[String])] = Nil, headers: Option[Headers] = None)(implicit decoder: Dec[A]): Context[A] = for {
    conf <- getConf
    request = Request[IO](
      method = method,
      uri = uri.copy(query = Query(params:_*)),
      headers = headers.getOrElse(conf.requestHeaders).withTraceToken(conf)
    )
    res <- client.use { _.expectOr[A](request)(errorHandler) }.context
  } yield res

  protected def doRequestWithBody[A, B](method: PermitsBody, uri: Uri, payload: A, headers: Option[Headers] = None)(implicit encoder: Enc[A], decoder: Dec[B]): Context[B] = for {
    conf <- getConf
    request = Request[IO](
      method = method,
      uri = uri,
      headers = headers.getOrElse(conf.requestHeaders).withTraceToken(conf)
    ).withEntity(payload)
    res <- client.use { _.expectOr[B](request)(errorHandler)(decoder) }.context
  } yield res

  // Default error handler. Override as required.
  protected def errorHandler(r: Response[IO]): IO[Throwable] = r.status match {
    case Status.NotFound => r.body.drain; IO(NotFound())
    case Status.Forbidden => r.body.drain; IO(Forbidden())
    case Status.Unauthorized => r.body.drain; IO(Unauthorized())
    case Status.BadRequest => EntityDecoder.decodeString(r).map(BadRequest)
    case s => r.body.drain; IO(GatewayError(s"Unexpected response. Got status: ${s.code}"))
  }
}

trait BaseCrudClient extends BaseHttpClient {
  protected val uri: Uri
}

trait Create[A, B] extends BaseCrudClient {
  def create(a: A)(implicit encoder: Enc[A], decoder: Dec[B]): Context[B] = doRequestWithBody[A, B](POST, uri, a)
}

trait Read[A] extends BaseCrudClient {
  def get(id: String, params: (String, Option[String])*)(implicit decoder: Dec[A]): Context[A] =
    doRequestEmptyBody(GET, uri / id, params)
}

trait Update[A, B] extends BaseCrudClient {
  def update(id: String, a: A)(implicit encoder: Enc[A], decoder: Dec[B]): Context[B] =
    doRequestWithBody[A, B](PUT, uri / id, a)
}

trait Delete extends BaseCrudClient {
  def delete(id: String): Context[Unit] = doRequestEmptyBody(DELETE, uri / id)
}

trait Patch[A] extends BaseCrudClient {
  def patch(id: String, a: A)(implicit encoder: Enc[A]): Context[Unit] =
    doRequestWithBody[A, Unit](PATCH, uri / id, a)
}