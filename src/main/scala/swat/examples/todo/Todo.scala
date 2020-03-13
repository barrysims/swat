package swat.examples.todo

import io.circe.generic.JsonCodec

@JsonCodec
case class Todo(userId: Long, id: Long, title: String, completed: Boolean)

@JsonCodec
case class WritableTodo(userId: Long, title: String, completed: Boolean)
