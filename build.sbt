import com.typesafe.sbt.SbtGit
import sbt._

name := "swat"
version := "0.1"
scalaVersion := "2.12.10"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Dependencies.all

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-explaintypes",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Ywarn-unused:imports",
  "-Ypartial-unification"
)

scalacOptions in (Compile, console) ~= (_.filterNot(Set(
  "-Ywarn-unused:imports",
  "-Xfatal-warnings"
)))

fork := true
Test / testForkedParallel := true
IntegrationTest / fork := true

// Use sbt-git plugin to get hash of latest commit.
SbtGit.useJGit // Force JGit for portability
val gitHash = SettingKey[String]("gitCommit")
gitHash := git.gitHeadCommit.value.getOrElse("UNKNOWN")

// Set Git SHA in BuildInfo.
enablePlugins(sbtbuildinfo.BuildInfoPlugin)
buildInfoKeys ++= Seq[BuildInfoKey](BuildInfoKey.action("gitHash") { gitHash.value })
buildInfoPackage := "build"

