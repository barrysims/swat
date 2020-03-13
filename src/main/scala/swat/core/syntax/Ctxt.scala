package swat.core.syntax

import cats.data.{EitherT, Kleisli, Reader}
import cats.effect.{ContextShift, IO}
import cats.implicits._
import io.circe.Encoder
import org.http4s.circe.CirceEntityCodec._
import org.http4s.headers.`Content-Type`
import org.http4s.rho.RhoRoutes
import org.http4s.rho.bits.EntityResponseGenerator
import org.http4s.{Header, Headers, InvalidMessageBodyFailure, MalformedMessageBodyFailure, MediaType}
import org.slf4j.LoggerFactory
import swat.core.syntax.conf.Conf

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

/**
  * Transformers for monad stack Reader[IO[Either]]
  * Has two type parameters, one for the reader configuration and one for the reader result type
  */
trait CtxtBase {
  type Context2[A, C <: Conf] = Kleisli[EitherTIO, C, A]
  type EitherTIO[A] = EitherT[IO, Throwable, A]
}

object ConfSyntax extends RhoRoutes[IO]

/**
  * Transformers for monad stack Reader[IO[Either]]
  * Functions have one type parameter: the reader result type
  * The reader configuration type is provided by the type parameter of the Ctxt trait
  * This allows us to extend this trait and declare the configuration type once for any subclass
  */
trait Ctxt[CONF <: Conf] extends CtxtBase with OtherSyntax {

  // Type aliases for the monad stack
  type Context[A] = Kleisli[EitherTIO, CONF, A]

  val getConf: Kleisli[EitherTIO, CONF, CONF] = Kleisli.ask[EitherTIO, CONF]

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

  // Conversions, for lifting up the monad stack
  implicit class LiftIO[A](io: IO[A]) {
    def context: Context[A] = Kleisli[EitherTIO, CONF, A](_ => EitherT(io.attempt))
  }
  implicit class LiftStack[A](ioEither: IO[Either[Throwable, A]]) {
    def context: Context[A] = Kleisli[EitherTIO, CONF, A](_ => EitherT(ioEither))
  }
  implicit class LiftEither[A](eitherA: Either[Throwable, A]) {
    def context: Context[A] = Kleisli[EitherTIO, CONF, A](_ => EitherT[IO, Throwable, A](IO(eitherA)))
  }
  implicit class LiftEitherT[A](eitherT: EitherT[IO, Throwable, A]) {
    def context: Context[A] = Kleisli[EitherTIO, CONF, A](_ => eitherT)
  }
  implicit class ReaderOps[A](r: Reader[CONF, IO[Either[Throwable, A]]]) {
    def context: Context[A] = Kleisli[EitherTIO, CONF, A](conf => EitherT(r.run(conf).extract))
  }
  implicit class IdentOps[A](a: A) {
    def context: Context[A] = Kleisli[EitherTIO, CONF, A](_ => EitherT[IO, Throwable, A](IO(Right(a))))
  }

  private def logEither[A](logger: OtherSyntax.LogMethod)(e: Either[Throwable, A]): Either[Throwable, A] =
    e.fold(
      l => { logger(l.getMessage); Left(l) },
      r => Right(r)
    )

  implicit class SeqContextOps[A](l: List[Context[A]]) {
    def filterFailures(logger: OtherSyntax.LogMethod = OtherSyntax.noOpLogger): Context[List[A]] = for {
      conf <- getConf
      res <- l.map { _.run(conf).value }.parSequence.map(_.map(logEither(logger))).map { _.collect { case Right(a) => a } }.context
    } yield res
  }

  implicit class ContextOps[A](c: Context[A]) {
    // Exposes the either without failing the Context
    def extractEither: Context[Either[Throwable, A]] = for {
      conf <- getConf
      res <- c.run(conf).value.map { Right(_) }.context
    } yield res

    // Runs a context with a temporary configuration
    def withConf(newConf: CONF): Context[A] = c.run(newConf).context
  }

  // Takes a tuple of Contexts and returns a Context of a tuple of the results, in which the IOs will run in parallel
  def runInParallel[A, B](contexts: (Context[A], Context[B])): Context[(A, B)] = {
    for {
      aEitherTIO <- contexts._1.toReader
      bEitherTIO <- contexts._2.toReader
    } yield (aEitherTIO.value, bEitherTIO.value).parMapN { (aEither, bEither) =>
      for {
        a <- aEither
        b <- bEither
      } yield (a, b)
    }
  }.context

  // Takes a tuple of Contexts and returns a Context of a tuple of the results, in which the IOs will run in parallel
  def runInParallel[A, B, C](contexts: (Context[A], Context[B], Context[C])): Context[(A, B, C)] = {
    for {
      aEitherTIO <- contexts._1.toReader
      bEitherTIO <- contexts._2.toReader
      cEitherTIO <- contexts._3.toReader
    } yield (aEitherTIO.value, bEitherTIO.value, cEitherTIO.value).parMapN { (aEither, bEither, cEither) =>
      for {
        a <- aEither
        b <- bEither
        c <- cEither
      } yield (a, b, c)
    }
  }.context

  import ConfSyntax._

  implicit class ConfOps(conf: Conf) {

    def requestHeaders: Headers = Headers(List(
      Header(ApiHeader.TraceToken, conf.traceToken),
      `Content-Type`(MediaType.application.json)
    ))
  }

  private val log = LoggerFactory.getLogger(this.getClass)

  protected def mapError(t: Throwable) = t match {
    case Error.Conflict(m) => Conflict(ErrorMessage(m))
    case Error.NotFound(m) => NotFound(ErrorMessage(m))
    case Error.BadRequest(m) => BadRequest(ErrorMessage(m))
    case Error.Unauthorized(m) => Unauthorized(ErrorMessage(m))
    case Error.GatewayError(m, _) => BadGateway(ErrorMessage(m))
    case Error.UnexpectedError(m, _) => InternalServerError(ErrorMessage(m))
    case InvalidMessageBodyFailure(details, cause) =>
      log.error(s"Failed to decode JSON $details $cause")
      InternalServerError(ErrorMessage(s"Failed to decode JSON"))
    case MalformedMessageBodyFailure(details, cause) =>
      log.error(s"Malformed JSON $details $cause")
      BadGateway(ErrorMessage("malformed message body"))
    case NonFatal(e) => InternalServerError(ErrorMessage(e.getMessage))
  }

  implicit class ContextToResponse[A, C <: Conf](context: Kleisli[EitherTIO, C, A])(implicit encoder: Encoder[A]) {
    def toResponse(conf: C, status: EntityResponseGenerator[IO] = Ok) = {
      val result = context.run(conf).value.flatMap {
        case Right(r) => status(r)
        case Left(error) => mapError(error)
      }
      result.map(_.putHeaders(conf.responseHeaders.toList: _*))
    }
  }
}

