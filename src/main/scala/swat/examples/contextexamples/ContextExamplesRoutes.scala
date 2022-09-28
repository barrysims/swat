package swat.examples.contextexamples

import cats.effect.IO
import org.http4s.Request
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._
import swat.core.syntax.Ctxt
import swat.core.syntax.conf.SimpleConf

class ContextExamplesRoutes(val contextExamplesService: ContextExamplesService = new ContextExamplesService)
  extends RhoRoutes[IO] with Ctxt[SimpleConf] {

  "Do Something" ** GET / "something" / pathVar[Int]("n") |>> { (r: Request[IO], n: Int) =>
    contextExamplesService.doSomething(n).toResponse(SimpleConf.fromRequest(r))
  }
}
