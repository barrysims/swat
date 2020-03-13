package swat.examples.todo

import org.http4s.Method.GET
import org.http4s.Uri
import swat.core.clients.httpclient.{Create, Delete, Read, Update}
import org.http4s.circe.CirceEntityCodec._
import swat.core.conf.Static


class TodoClient extends Create[WritableTodo, Todo] with Read[Todo] with Update[WritableTodo, Todo] with Delete {

  override val uri: Uri = Static.config.dummyApi.url / "todos"

  // getAll is non-standard REST so we declare it here
  def getAll()(implicit decoder: Dec[Todo]): Context[Seq[Todo]] = doRequestEmptyBody[Seq[Todo]](GET, uri, Nil)
}
