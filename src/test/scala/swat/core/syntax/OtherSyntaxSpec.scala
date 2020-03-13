package swat.core.syntax

import org.scalatest.{FlatSpec, Matchers}
import swat.core.syntax.OtherSyntax._

class OtherSyntaxSpec extends FlatSpec with Matchers {

  private case class SomeError(msg: String = "Error") extends Throwable

  behavior of "orFail"

  it should "return a Right containing the value of the Some" in {
    Some(3).orFail(SomeError()) shouldBe Right(3)
  }

  it should "return a Left containing the specified Throwable for a None" in {
    None.orFail(SomeError()) shouldBe Left(SomeError())
  }
}
