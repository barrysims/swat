package swat.examples.todo

import swat.core.syntax.Ctxt
import swat.core.syntax.conf.SimpleConf
import org.http4s.circe.CirceEntityCodec._


class TodoService(client: TodoClient = new TodoClient) extends Ctxt[SimpleConf] {

  def get(): Context[Seq[Todo]] = client.getAll()

  def get(id: String): Context[Todo] = client.get(id)

  def create(todo: WritableTodo): Context[Todo] = client.create(todo)

  def update(id: String, todo: WritableTodo): Context[Todo] = client.update(id, todo)

  def delete(id: String): Context[Unit] = client.delete(id)
}
