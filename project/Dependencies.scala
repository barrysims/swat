import sbt._

object Dependencies {

  private val http4sVersion = "0.21.1"
  private val rhoVersion = "0.20.0"
  private val circeVersion = "0.12.3"
  private val doobieVersion = "0.8.8"
  private val enumeratumVersion = "1.5.13"
  private val enumeratumCirceVersion = "1.5.18"

  private val libraries = Seq(

    "org.tpolecat" %% "doobie-core" % doobieVersion,

    "org.tpolecat" %% "doobie-h2" % doobieVersion,
    "org.tpolecat" %% "doobie-hikari" % doobieVersion,
    "org.tpolecat" %% "doobie-postgres" % doobieVersion,

    "org.typelevel" %% "cats-core" % "1.4.0",
    "org.typelevel" %% "cats-effect" % "0.10",

    "org.http4s" %% "http4s-core" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
    "org.http4s" %% "http4s-jetty" % http4sVersion,

    "org.http4s" %% "http4s-client" % http4sVersion,
    "org.http4s" %% "http4s-async-http-client" % http4sVersion,

    "org.http4s" %% "rho-core" % rhoVersion,
    "org.http4s" %% "rho-swagger" % rhoVersion,

    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-literal" % circeVersion,

    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "net.logstash.logback" % "logstash-logback-encoder" % "6.0",

    "com.github.tototoshi" %% "scala-csv" % "1.3.5",

    "com.typesafe" % "config" % "1.3.2",
    "com.github.pureconfig" %% "pureconfig" % "0.10.2",

    "com.github.blemale" %% "scaffeine" % "2.5.0"
  )

  private val commonTestDependencies = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "org.mockito" % "mockito-core" % "2.19.1" % "test"
  )

  val all = libraries ++ commonTestDependencies
}
