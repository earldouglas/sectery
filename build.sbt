val scala3Version = "3.0.1"
val zioVersion = "2.0.0-M3"

val enableScalafix =
  List(
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0",
    scalaVersion := scala3Version,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions += "-Xlint:unused" // unsupported in Scala 3, but required by scalafix
  )

inThisBuild(enableScalafix)

resolvers += "jitpack" at "https://jitpack.io/" // needed for pircbotx

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(GitVersioning)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "sectery",
    scalaVersion := scala3Version,
    libraryDependencies += "org.json4s" %% "json4s-native-core" % "4.0.3",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.3.0",
    libraryDependencies += "org.jsoup" % "jsoup" % "1.14.3",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6",
    libraryDependencies += "com.github.pircbotx" % "pircbotx" % "2.2",
    libraryDependencies += "dev.zio" %% "zio" % zioVersion,
    libraryDependencies += "dev.zio" %% "zio" % zioVersion,
    libraryDependencies += "dev.zio" %% "zio-test" % zioVersion % "test",
    libraryDependencies += "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
    libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.36.0.3" % "test",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    Compile / run / fork := true,
    Test / fork := true,
    Test / envVars :=
      Map(
        "AIRNOW_API_KEY" -> "alligator3",
        "DARK_SKY_API_KEY" -> "alligator3",
        "FINNHUB_API_TOKEN" -> "alligator3"
      ),
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "sectery"
  )
