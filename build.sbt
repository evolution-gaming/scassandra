import Dependencies.*

ThisBuild / versionScheme := Some("early-semver")
ThisBuild / versionPolicyIntention := Compatibility.BinaryCompatible

def crossSettings[T](scalaVersion: String, if3: Seq[T], if2: Seq[T]): Seq[T] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((3, _)) => if3
    case Some((2, _)) => if2
    case _ => Nil
  }

lazy val commonSettings = Seq(
  organization := "com.evolutiongaming",
  homepage := Some(url("https://github.com/evolution-gaming/scassandra")),
  startYear := Some(2018),
  organizationName := "Evolution",
  organizationHomepage := Some(url("https://evolution.com")),
  scalaVersion := crossScalaVersions.value.head,
  crossScalaVersions := Seq("2.13.18", "3.3.8"),
  Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings"),
  publishTo := Some(Resolver.evolutionReleases),
  licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT"))),
  scalacOptions ++= Seq(
    "-release:11",
    "-deprecation",
  ),
  scalacOptions ++= crossSettings(
    scalaVersion.value,
    if2 = Seq(
      "-Xsource:3",
    ),
    // Good compiler options for Scala 2.13 are coming from com.evolution:sbt-scalac-opts-plugin:0.0.9,
    // but its support for Scala 3 is limited, especially what concerns linting options.
    //
    // If Scala 3 is made the primary target, good linting scalac options for it should be added first.
    if3 = Seq(
      // used language features
      "-Ykind-projector:underscores",
      "-language:implicitConversions",

      // disable new brace-less syntax:
      // https://alexn.org/blog/2022/10/24/scala-3-optional-braces/
      "-no-indent",

      // improve error messages:
      "-explain",
      "-explain-types",
      "-feature",
    ),
  ),
)

addCommandAlias("fmt", "+all scalafmtAll scalafmtSbt")
// check needed for Evo workflows/release.yml - it adds '+' before calling check,
// tests are executed separately
addCommandAlias("check", "all scalafmtCheckAll scalafmtSbtCheck versionPolicyCheck Compile/doc")
addCommandAlias("build", "; +check; +test")

lazy val root = (project in file("."))
  .settings(name := "scassandra")
  .settings(commonSettings)
  .settings(
    publish / skip := true,
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
      jffi,
      `executor-tools`,
      Pureconfig.cats,
    ),
  )
  .settings(
    libraryDependencies ++= crossSettings(
      scalaVersion.value,
      if3 = List(Pureconfig.core),
      if2 = List(Pureconfig.pureconfig),
    ),
  )

lazy val tests = (project in file("tests"))
  .settings(name := "tests")
  .settings(commonSettings)
  .settings(
    Seq(
      publish / skip := true,
      Test / fork := true,
      Test / parallelExecution := false,
    ),
  )
  .dependsOn(scassandra % "test->test;compile->compile")
  .settings(
    libraryDependencies ++= Seq(
      `testcontainers-cassandra` % Test,
      Slf4j.api % Test,
      Slf4j.`log4j-over-slf4j` % Test,
      Logback.core % Test,
      Logback.classic % Test,
      scalatest % Test,
    ),
  )
