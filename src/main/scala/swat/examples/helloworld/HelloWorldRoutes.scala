package swat.examples.helloworld

import cats.effect.IO
import org.http4s.Request
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.syntax.io._
import swat.core.syntax.Ctxt
import swat.core.syntax.conf.SimpleConf

class HelloWorldRoutes(val helloWorldService: HelloWorldService = new HelloWorldService)
  extends RhoRoutes[IO] with Ctxt[SimpleConf] {

  "Hello World" ** GET / "helloworld" |>> { r: Request[IO] => helloWorldService.hello().context.toResponse(SimpleConf.fromRequest(r)) }

}

