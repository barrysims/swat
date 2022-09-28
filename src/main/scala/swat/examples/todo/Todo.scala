package swat.examples.todo

import io.circe.generic.JsonCodec

@JsonCodec
case class Todo(id: Long, userId: Long, title: String, completed: Boolean)

@JsonCodec
case class WritableTodo(userId: Long, title: String, completed: Boolean)
