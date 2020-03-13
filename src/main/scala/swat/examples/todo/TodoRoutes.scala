package swat.examples.todo

import cats.effect.IO
import org.http4s.Request
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._
import swat.core.syntax.Ctxt
import swat.core.syntax.conf.SimpleConf
import org.http4s.circe._


class TodoRoutes(val todoService: TodoService = new TodoService)
  extends RhoRoutes[IO] with Ctxt[SimpleConf] {

  "Get Todos" ** GET / "todo" |>> { r: Request[IO] => todoService.get().toResponse(SimpleConf.fromRequest(r)) }

  "Get Todo" ** GET / "todo" / pathVar[String]("id") |>> { (r: Request[IO], id: String) =>
    todoService.get(id).toResponse(SimpleConf.fromRequest(r))
  }

  "Create Todo" ** POST / "todo" ^ jsonOf[IO, WritableTodo] |>> { (r: Request[IO], todo: WritableTodo) =>
    todoService.create(todo).toResponse(SimpleConf.fromRequest(r))
  }

  "Update Todo" ** PUT / "todo" / pathVar[String]("id") ^ jsonOf[IO, WritableTodo] |>> { (r: Request[IO], id: String, todo: WritableTodo) =>
    todoService.update(id, todo).toResponse(SimpleConf.fromRequest(r))
  }

  "Delete Todo" ** DELETE / "todo" / pathVar[String]("id") |>> { (r: Request[IO], id: String) =>
    todoService.delete(id).toResponse(SimpleConf.fromRequest(r))
  }
}