# SWAT

![alt text](/fly.png "Scala Web Application Template")

Scala Web Application Template

This is a stripped down skeleton/framework project that I've been using to build web applications with Scala.

## Building, Testing, and Running

`sbt compile`

`sbt test`

`sbt run`

A couple of example services are bundled: `http://localhost:5000/api/v1/hello` and `http://localhost:5000/api/v1/todo`

Swagger and SwaggerUI are generated for both at `http://localhost:5000/swagger.json` and `http://localhost:5000/swagger`

SwaggerUI can be used to test the services.

## Aims of this Project

To provide a clean, boilerplate free way of building web services with scala that centralise error handling, logging, and swagger generation.
The heavy lifting is done by Rho/Http4s/Circe/Cats, but this project exposes a Monad stack `Ctxt` that allows us to manage three contexts:

* `Per-request configuration`
* `Effects`
* `Failure`

Using a monad stack `Reader[IO[Either[Throwable, A]]]`. (In fact, for simplicity, we usually just use the Transformer `Kleisli[EitherT[IO, Throwable, A], C, A]`)  

`trait Ctxt` defines a handful of useful syntaxes including a
`Context` typealias which acts as a transformer for the stack, and `.context` extensions which allow us to lift easily to a `Context`.

Additionally, a `toResponse` function is provided on `Context[A]` which handles
running the Reader and folding over the Either to generate an IO response with
an `OK` or standard error status.
  
## Organisation

swat/examples shows the code in use.  I've been using a very simple `routes -> service -> client` structure that isolates declaring the API (routes) from the business logic (service) from the persistence (client).

Most of the interesting Type mangling happens in swat/core/syntax/Ctxt.scala and this is used by adding `extends Ctxt[C]` to the code, where `C` is whatever configuration that needs to be passed around (in the examples it's just a simple object that wraps a trace-token).

 





