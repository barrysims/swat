package swat.core.syntax

object Error {

  sealed class ApiError(msg: String, cause: Option[Throwable] = None) extends RuntimeException(msg, cause.orNull)

  case class Conflict(msg: String = "Conflict") extends ApiError(msg)
  case class NotFound(msg: String = "Not Found") extends ApiError(msg)
  case class Forbidden(msg: String = "Forbidden") extends ApiError(msg)
  case class BadRequest(msg: String = "Bad Request") extends ApiError(msg)
  case class Unauthorized(msg: String = "Unauthorized") extends ApiError(msg)
  case class UnexpectedError(msg: String, cause: Option[Throwable] = None) extends ApiError(msg, cause)
  case class GatewayError(msg: String, cause: Option[Throwable] = None) extends ApiError(msg, cause)
}
