package swat.core.syntax

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import org.http4s._
import org.http4s.rho.Result
import org.http4s.syntax.StringSyntax
import org.scalatest.{EitherValues, FlatSpec, Matchers}
import swat.core.syntax.Error.UnexpectedError
import swat.core.syntax.conf.{Conf, SimpleConf}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CtxtSpec extends FlatSpec with Matchers with TestSyntax with EitherValues {

  trait TestContext extends Ctxt[Conf]

  behavior of "getConf"

  it should "retrieve the conf passed into run" in new TestContext {
    val testConf = SimpleConf(traceToken = "Some Tracetoken")
    getConf.run(testConf).value.unsafeRunSync() shouldBe Right(testConf)
  }

  behavior of "extractEither"

  it should "return a Right(Right(a)) given a successful Context[A]" in new TestContext {
    Right("foo").context.extractEither.evaluate shouldBe Right(Right("foo"))
  }

  it should "return a Right(Left(a)) given a failed Context[A]" in new TestContext {
    Left(UnexpectedError("Error")).context.extractEither.evaluate shouldBe Right(Left(UnexpectedError("Error")))
  }

  behavior of "runInParallel(Tuple2())"

  it should "return a tuple2 containing the results of the two contexts passed in" in new TestContext {
    val c1: Context[Int] = Right(1).context
    val c2: Context[Int] = Right(2).context
    val result: Either[Throwable, (Int, Int)] = runInParallel(c1, c2).evaluate
    result shouldBe Right(1, 2)
  }

  it should "run both contexts in parallel" in new TestContext {
    // Set up two futures, such that the second completes before the first. This allows us to confirm that the contexts
    // are evaluated in parallel.
    var counter = 0
    val f1 = Future {
      Thread.sleep(500)
      counter = counter + 1
      Right(counter)
    }
    val f2 = Future {
      counter = counter + 1
      Right(counter)
    }
    val result: Either[Throwable, (Int, Int)] = runInParallel(IO.fromFuture(IO(f1)).context, IO.fromFuture(IO(f2)).context).evaluate
    // If the futures are executed in parallel, f1 should be 2 and f2 should be 1 since f2 completes before f1.
    result shouldBe Right(2, 1)
  }

  behavior of "run in parallel(Tuple3())"

  it should "return a tuple3 containing the results of the three contexts passed in" in new TestContext {
    val c1: Context[Int] = Right(1).context
    val c2: Context[Int] = Right(2).context
    val c3: Context[Int] = Right(3).context
    val result: Either[Throwable, (Int, Int, Int)] = runInParallel(c1, c2, c3).evaluate
    result shouldBe Right(1, 2, 3)
  }

  it should "run three contexts in parallel" in new TestContext {
    var counter = 0
    // Set up three futures that complete in reverse order, to verify that they are evaluated in parallel.
    val f1 = Future {
      Thread.sleep(2000)
      counter = counter + 1
      Right(counter)
    }
    val f2 = Future {
      Thread.sleep(1000)
      counter = counter + 1
      Right(counter)
    }
    val f3 = Future {
      counter = counter + 1
      Right(counter)
    }
    val result: Either[Throwable, (Int, Int, Int)] = runInParallel(
      IO.fromFuture(IO(f1)).context,
      IO.fromFuture(IO(f2)).context,
      IO.fromFuture(IO(f3)).context
    ).evaluate
    // If the futures are executed in parallel, they should complete in the order f3, f2, f1.
    result shouldBe Right(3, 2, 1)
  }

  behavior of "filterFailures"

  it should "sequence, removing Lefts" in {
    List[Context[Int]](Right(1).context, Right(2).context).filterFailures().evaluate.right.value shouldBe List(1, 2)
    List[Context[Int]](Right(1).context, Left(new RuntimeException()).context).filterFailures().evaluate.right.value shouldBe List(1)
    List[Context[Int]]().filterFailures().evaluate.right.value shouldBe List()
  }

  behavior of "toConf"

  it should "return a conf populated with the trace token and auth headers from the incoming request" in {
    SimpleConf.fromRequest(request(Some("12345"))) shouldBe SimpleConf("12345")
  }

  it should "generate a trace token if none is set on the incoming request" in {
    val result = SimpleConf.fromRequest(request(None))
    result.traceToken.isEmpty shouldBe false
  }

  behavior of "toResponse"

  trait ResponseTestContext extends StringSyntax

  it should "return a successful response with tracetoken header set" in new ResponseTestContext {
    val successfulContext = Kleisli[EitherTIO, SimpleConf, Unit]{ _ => EitherT[IO, Throwable, Unit](IO(Right(()))) }

    successfulContext.toResponse(SimpleConf("12345")).unsafeRunSync() match {
      case Result(r) =>
        r.status shouldBe Status.Ok
        r.headers.get(ApiHeader.TraceToken.ci) shouldBe Some(Header(ApiHeader.TraceToken, "12345"))
    }
  }

  it should "translate a BadRequest ApiError into a Response with the correct HTTP status" in new ResponseTestContext {
    val badRequest = Kleisli[EitherTIO, SimpleConf, Unit]{ _ => EitherT[IO, Throwable, Unit](IO(Left(Error.BadRequest())))}

    badRequest.toResponse(SimpleConf("12345")).unsafeRunSync() match {
      case Result(r) =>
        r.status shouldBe Status.BadRequest
        r.headers.get(ApiHeader.TraceToken.ci) shouldBe Some(Header(ApiHeader.TraceToken, "12345"))
    }
  }

  it should "translate a NonFatal exception into an internal server error response" in new ResponseTestContext {
    val failedRequest = Kleisli[EitherTIO, SimpleConf, Unit]{ _ => EitherT[IO, Throwable, Unit](IO(Left(new IllegalArgumentException("Error"))))}

    failedRequest.toResponse(SimpleConf("12345")).unsafeRunSync() match {
      case Result(r) =>
        r.status shouldBe Status.InternalServerError
        r.headers.get(ApiHeader.TraceToken.ci) shouldBe Some(Header(ApiHeader.TraceToken, "12345"))
    }
  }

  private def request(traceToken: Option[String]) = Request[IO](
    method = Method.GET,
    headers = traceToken.fold(Headers.of()){tt => Headers.of(Header(ApiHeader.TraceToken, tt))}
  )
}
