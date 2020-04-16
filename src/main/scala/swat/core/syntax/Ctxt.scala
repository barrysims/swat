package swat.core.syntax

import cats.data.{Kleisli, Reader}
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


object ConfSyntax extends RhoRoutes[IO]

/**
  * Transformers for monad stack Reader[IO[A]]
  * Functions have one type parameter: the reader result type
  * The reader configuration type is provided by the type parameter of the Ctxt trait
  * This allows us to extend this trait and declare the configuration type once for any subclass
  */
trait Ctxt[CONF <: Conf] extends OtherSyntax {

  // Type aliases for the monad stack
  type Context[A] = Kleisli[IO, CONF, A]

  val getConf: Kleisli[IO, CONF, CONF] = Kleisli.ask[IO, CONF]

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

  // Conversions, for lifting up the monad stack
  implicit class LiftIO[A](io: IO[A]) {
    def context: Context[A] = Kleisli[IO, CONF, A](_ => io)
  }
  implicit class EitherOps[A](e: Either[Throwable, A]) {
    def context: Context[A] = Kleisli[IO, CONF, A](_ => IO.fromEither(e))
  }
  implicit class ReaderOps[A](r: Reader[CONF, IO[A]]) {
    def context: Context[A] = Kleisli[IO, CONF, A](conf => r.run(conf))
  }
  implicit class IdentOps[A](a: A) {
    def context: Context[A] = Kleisli[IO, CONF, A](_ => IO.delay(a))
  }

  private def logEither[A](logger: OtherSyntax.LogMethod)(e: Either[Throwable, A]): Either[Throwable, A] =
    e.fold(
      l => { logger(l.getMessage); Left(l) },
      r => Right(r)
    )

  implicit class SeqContextOps[A](l: List[Context[A]]) {
    def filterFailures(logger: OtherSyntax.LogMethod = OtherSyntax.noOpLogger): Context[List[A]] = for {
      conf <- getConf
      res <- l.map { _.run(conf).attempt }.parSequence.map(_.map(logEither(logger))).map { _.collect { case Right(a) => a } }.context
    } yield res
  }

  implicit class ContextOps[A](c: Context[A]) {
    // Exposes the either without failing the Context
    def extractEither: Context[Either[Throwable, A]] = for {
      conf <- getConf
      res <- c.run(conf).attempt.context
    } yield res

    // Runs a context with a temporary configuration
    def withConf(newConf: CONF): Context[A] = c.run(newConf).context
  }

  // Takes a tuple of Contexts and returns a Context of a tuple of the results, in which the IOs will run in parallel
  def runInParallel[A, B](contexts: (Context[A], Context[B])): Context[(A, B)] = {
    for {
      aEitherTIO <- contexts._1.toReader
      bEitherTIO <- contexts._2.toReader
    } yield (aEitherTIO, bEitherTIO).parMapN { (_, _) }
  }.context

  // Takes a tuple of Contexts and returns a Context of a tuple of the results, in which the IOs will run in parallel
  def runInParallel[A, B, C](contexts: (Context[A], Context[B], Context[C])): Context[(A, B, C)] = {
    for {
      aEitherTIO <- contexts._1.toReader
      bEitherTIO <- contexts._2.toReader
      cEitherTIO <- contexts._3.toReader
    } yield (aEitherTIO, bEitherTIO, cEitherTIO).parMapN { (_, _, _) }
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

  implicit class ContextToResponse[A, C <: Conf](context: Kleisli[IO, C, A])(implicit encoder: Encoder[A]) {
    def toResponse(conf: C, status: EntityResponseGenerator[IO] = Ok) = {
      val result = context.run(conf).attempt.flatMap {
        case Right(r) => status(r)
        case Left(error) => mapError(error)
      }
      result.map(_.putHeaders(conf.responseHeaders.toList: _*))
    }
  }
}

