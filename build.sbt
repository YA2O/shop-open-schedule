val Http4sVersion = "0.23.1"
val CirceVersion = "0.14.6"
val CirisVersion = "2.1.1"
val LogbackVersion = "1.2.5"
val ScalaTestVersion = "3.2.13"

lazy val root = (project in file("."))
  .settings(
    organization := "bzh.ya2o",
    name := "opening-hours",
    version := "1.0.0",
    scalaVersion := "2.13.11",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "is.cir" %% "ciris" % CirisVersion,
      "is.cir" %% "ciris-refined" % CirisVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
    ),
    Test / run / fork := true
  )

Compile / mainClass := Some("bzh.ya2o.openinghours.Main")
Docker / packageName := "ya2o/opening-hours"
dockerBaseImage := "openjdk:17-jdk"
dockerExposedPorts ++= Seq(8080)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias("check", "clean; scalafmt; scalafmtSbt; test; doc")