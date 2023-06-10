import Dependencies._

lazy val commonSettings = Seq(
  organization := "com.evolutiongaming",
  homepage := Some(new URL("http://github.com/evolution-gaming/scassandra")),
  startYear := Some(2018),
  organizationName := "Evolution",
  organizationHomepage := Some(url("http://evolution.com")),
  scalaVersion := crossScalaVersions.value.head,
  crossScalaVersions := Seq("2.13.11", "2.12.17"),
  Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings"),
  publishTo := Some(Resolver.evolutionReleases),
  licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT"))),
  releaseCrossBuild := true,
  scalacOptsFailOnWarn := Some(false))

lazy val root = (project in file(".")
  settings (name := "scassandra")
  settings commonSettings
  settings (
    publish / skip := true,
    skip / publishArtifact := true)
  aggregate(scassandra, tests))

lazy val scassandra = (project in file("scassandra")
  settings (name := "scassandra")
  settings commonSettings
  settings (libraryDependencies ++= Seq(
    Cats.core,
    Cats.effect,
    `config-tools`,
    `cats-helper`,
    sstream,
    scalatest % Test,
    nel,
    `cassandra-driver`,
    `executor-tools`,
    Pureconfig.pureconfig,
    Pureconfig.cats)))

lazy val tests = (project in file("tests")
  settings (name := "tests")
  settings commonSettings
  settings Seq(
    publish / skip := true,
    skip / publishArtifact := true,
    Test / fork := true,
    Test / parallelExecution := false)
  dependsOn scassandra % "test->test;compile->compile"
  settings (libraryDependencies ++= Seq(
    `cassandra-launcher` % Test,
    Slf4j.api % Test,
    Slf4j.`log4j-over-slf4j` % Test,
    Logback.core % Test,
    Logback.classic % Test,
    scalatest % Test)))
