package swat.examples.contextexamples

import cats.effect.IO
import swat.core.syntax.Ctxt
import swat.core.syntax.conf.SimpleConf
import cats.implicits._

import scala.util.Random

class ContextExamplesService extends Ctxt[SimpleConf] {

  def doSomething(n: Int): Context[Int] = for {
    n1 <- doSomethingTrivial(n).context
    n2 <- doSomethingThatCanFail(n1).context
    n3 <- doSomethingImpure(n2).context
    n4 <- doSomethingThatNeedsConfig(n3)
  } yield n4

  private def doSomethingTrivial(n: Int): Int = n + 1

  private def doSomethingThatCanFail(n: Int): Either[Throwable, Int] = Either.catchNonFatal(n / Random.nextInt(2))

  private def doSomethingImpure(n: Int): IO[Int] = IO.delay(n)

  private def doSomethingThatNeedsConfig(n: Int): Context[Int] = for {
    c <- getConf
  } yield n + c.traceToken.hashCode
}
