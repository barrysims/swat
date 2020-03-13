package swat.core.syntax

import cats.effect.IO
import cats.syntax.either._
import org.http4s.{EntityDecoder, EntityEncoder, Uri}
import swat.core.syntax.Error.{NotFound, UnexpectedError}

trait OtherSyntax {

  type EitherThrowable[A] = Either[Throwable, A]

  type Enc[A] = EntityEncoder[IO, A]
  type Dec[A] = EntityDecoder[IO, A]

  trait Transformer[A, B] {
    def transform(a: A): B
  }

  implicit class IdentityOp[A](a: A) {
    def toOption(condition: A => Boolean): Option[A] = condition(a).toOption(a)
    def right: EitherThrowable[A] = Right[Throwable, A](a)
    def println: A = { System.out.println(a); a}
  }

  implicit class OptOps[A](opt: Option[A]) {
    def orFail(t: Throwable): Either[Throwable, A] = Either.fromOption(opt, t)
    def onNone(f: => Unit): Option[A] = opt.orElse({ f; None })
  }

  implicit class OptEitherOps[A](optEither: Option[Either[Throwable, A]]) {
    def toEitherOpt: EitherThrowable[Option[A]] = optEither.fold[EitherThrowable[Option[A]]](Right(None)) {
      case Left(t) => Left(t)
      case Right(a) => Right(Some(a))
    }
  }

  implicit class MapOps[A,B](map: Map[A,B]) {
    def getOrFail(k: A): Either[Throwable, B] = map.get(k).orFail(new NoSuchElementException(s"No entry found for ${k.toString}"))
  }

  implicit class ListOps[A](l: List[A]) {
    def unique(domainType: String): Either[Throwable, A] = l match {
      case h :: Nil => Right(h)
      case _ :: _ => Left(UnexpectedError(s"multiple ${domainType}s found"))
      case Nil => Left(NotFound(s"$domainType not found"))
    }
  }

  implicit class BooleanOps(bool: Boolean) {
    def toOption[A](ifTrue: A): Option[A] = if (bool) Option(ifTrue) else None
    def fold[A](ifFalse: A)(ifTrue: A): A = if (bool) ifTrue else ifFalse
  }

  implicit class StringOps(s: String) {
    // Fails with throwable, so only use in initialising code
    def / (right: String): Uri = Uri.unsafeFromString(s"$s/$right")
  }

  type LogMethod = String => Unit

  val noOpLogger: LogMethod = _ => ()

  private def logEither[A](logger: LogMethod)(e: Either[Throwable, A]): Either[Throwable, A] =
    e.fold(
      l => { logger(l.getMessage); Left(l) },
      r => Right(r)
    )

  implicit class ListEitherOps[A](l: List[Either[Throwable, A]]) {
    def filterFailures(logger: LogMethod = noOpLogger): List[A] = l.map { a => logEither(logger)(a) } collect {
      case Right(a) => a
    }
  }
}

object OtherSyntax extends OtherSyntax

