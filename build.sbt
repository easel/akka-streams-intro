organization := "org.github.easel"
name := "akka-streams-intro"
version := "0.1"

val scalaV = "2.11.8"

tutSettings

tutTargetDirectory := baseDirectory.value / "slides"

lazy val akkaStreamsIntro = project.in(file("."))
  .settings(scalaVersion := scalaV)
  .settings(libraryDependencies ++= Dependencies.Akka)
  .settings(libraryDependencies ++= Dependencies.AkkaHttp)
  .settings(libraryDependencies ++= Dependencies.Cats)
  .settings(libraryDependencies ++= Dependencies.ScalaJHttp)
  .settings(libraryDependencies ++= Dependencies.ScalaTest)
