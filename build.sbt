val scala3Version = "3.0.0"
val zioVersion = "1.0.9"

val enableScalafix =
  List(
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0",
    scalaVersion := "3.0.0",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions += "-Xlint:unused" // unsupported in Scala 3, but required by scalafix
  )

inThisBuild(enableScalafix)

resolvers += "jitpack" at "https://jitpack.io/"

lazy val root = project
  .in(file("."))
  .settings(
    name := "sectery",
    scalaVersion := scala3Version,
    libraryDependencies += "org.json4s" %% "json4s-native-core" % "4.0.0",
    libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.34.0",
    libraryDependencies += "org.jsoup" % "jsoup" % "1.13.1",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "com.github.pircbotx" % "pircbotx" % "2.2",
    libraryDependencies += "dev.zio" %% "zio" % zioVersion,
    libraryDependencies += "dev.zio" %% "zio" % zioVersion,
    libraryDependencies += "dev.zio" %% "zio-test" % zioVersion % "test",
    libraryDependencies += "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    Compile / run / fork := true,
    Compile / run / envVars :=
      Map(
        "IRC_USER" -> sys.env("IRC_USER"),
        "IRC_PASS" -> sys.env("IRC_PASS"),
        "IRC_HOST" -> sys.env("IRC_HOST"),
        "IRC_CHANNELS" -> sys.env("IRC_CHANNELS"),
        "DARK_SKY_API_KEY" -> sys.env("DARK_SKY_API_KEY")
      ),
    Test / fork := true,
    Test / envVars :=
      Map(
        "DARK_SKY_API_KEY" -> "alligator3"
      )
  )
