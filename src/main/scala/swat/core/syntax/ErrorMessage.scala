package swat.core.syntax

import io.circe.generic.JsonCodec

@JsonCodec
case class ErrorMessage(message: String)

