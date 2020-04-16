package swat.core.syntax

import java.util.UUID

import swat.core.syntax.conf.{Conf, SimpleConf}

trait TestSyntax extends Ctxt[Conf]{

  private def conf = SimpleConf(UUID.randomUUID().toString)

  implicit class ContextToOpsWithoutEncoder[A](context: Context[A]) {

    def get: A = toAWithConf(conf)
    def get(c: Conf): A = toAWithConf(c)

    def evaluate: Either[Throwable, A] = evaluate(conf)
    def evaluate(c: Conf): Either[Throwable, A] = context.run(c).attempt.unsafeRunSync()

    private def toAWithConf(c: Conf): A = evaluate(c).fold(
      { t: Throwable => throw t },
      identity
    )
  }
}
