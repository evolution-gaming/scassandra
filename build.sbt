import Dependencies._

ThisBuild / versionScheme := Some("early-semver")
ThisBuild / versionPolicyIntention := Compatibility.BinaryCompatible

def crossSettings[T](scalaVersion: String, if3: List[T], if2: List[T]) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((3, _)) => if3
    case Some((2, 12 | 13)) => if2
    case _ => Nil
  }

lazy val commonSettings = Seq(
  organization := "com.evolutiongaming",
  homepage := Some(url("http://github.com/evolution-gaming/scassandra")),
  startYear := Some(2018),
  organizationName := "Evolution",
  organizationHomepage := Some(url("https://evolution.com")),
  scalaVersion := crossScalaVersions.value.head,
  crossScalaVersions := Seq("2.13.16", "3.3.3", "2.12.19"),
  Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings"),
  publishTo := Some(Resolver.evolutionReleases),
  licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT"))),
  releaseCrossBuild := true,
  scalacOptsFailOnWarn := Some(false),
  scalacOptions ++= crossSettings(
    scalaVersion.value,
    if3 = List("-Ykind-projector", "-language:implicitConversions", "-explain", "-deprecation"),
    if2 = List("-Xsource:3"),
  ))

val alias: Seq[sbt.Def.Setting[?]] =
  //  addCommandAlias("check", "all versionPolicyCheck Compile/doc") ++
  addCommandAlias("check", "show version") ++
    addCommandAlias("build", "+all compile test")

lazy val root = (project in file("."))
  .settings(name := "scassandra")
  .settings(commonSettings)
  .settings(alias)
  .settings(
    publish / skip := true,
    skip / publishArtifact := true
  )
  .aggregate(scassandra, tests)

lazy val scassandra = (project in file("scassandra"))
  .settings(name := "scassandra")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      Cats.core,
      Cats.effect,
      `config-tools`,
      `cats-helper`,
      sstream,
      scalatest % Test,
      nel,
      `cassandra-driver`,
      `executor-tools`,
      Pureconfig.cats
    )
  )
  .settings(
    libraryDependencies ++= crossSettings(
      scalaVersion.value,
      if3 = List(Pureconfig.core),
      if2 = List(Pureconfig.pureconfig),
    )
  )

lazy val tests = (project in file("tests"))
  .settings(name := "tests")
  .settings(commonSettings)
  .settings(
    Seq(
      publish / skip := true,
      skip / publishArtifact := true,
      Test / fork := true,
      Test / parallelExecution := false
    )
  )
  .dependsOn(scassandra % "test->test;compile->compile")
  .settings(
    libraryDependencies ++= Seq(
      `testcontainers-cassandra` % Test,
      Slf4j.api % Test,
      Slf4j.`log4j-over-slf4j` % Test,
      Logback.core % Test,
      Logback.classic % Test,
      scalatest % Test
    )
  )
