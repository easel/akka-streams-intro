import sbt._

object Dependencies {
  object Versions {
    val Akka = "2.4.2"
    val AkkaStreamExtensions = "0.10.0"
    val Cats = "0.5.0"
    val RevealJs = "3.2.0"
    val ScalaTex = "0.3.5"
    val ScalaJHttp = "2.3.0"
    val Slick = "3.1.1"
  }

  val Akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % Versions.Akka,
    "com.typesafe.akka" %% "akka-stream" % Versions.Akka,
    "com.typesafe.akka" %% "akka-slf4j" % Versions.Akka,
    "com.mfglabs" %% "akka-stream-extensions" % Versions.AkkaStreamExtensions
  )

  val Cats = Seq(
    "org.typelevel" %% "cats" % Versions.Cats
  )

  val RevealJs = Seq(
    "org.webjars.bower" % "reveal.js" % Versions.RevealJs
  )

  val ScalaTexSite = Seq(
    "com.lihaoyi" %% "scalatex-site" %Versions.ScalaTex
  )

  val ScalaJHttp = Seq(
    "org.scalaj" %% "scalaj-http" % Versions.ScalaJHttp
  )

  val Slick = Seq(
    "com.typesafe.slick" %% "slick" % Dependencies.Versions.Slick,
    "com.h2database" % "h2" % "1.3.175",
    "org.xerial" % "sqlite-jdbc" % "3.8.10.1"
  )
}
