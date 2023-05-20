val zioVersion = "2.0.13"
val zioAwsVersion = "6.20.58.3"
val zioJsonVersion = "0.5.0"
val zioLoggingVersion = "2.1.12"

ThisBuild / scalaVersion := "3.2.2"

ThisBuild / assembly / assemblyMergeStrategy := {
  case "module-info.class"                     => MergeStrategy.first
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val shared =
  project
    .in(file("modules/shared"))
    .settings(
      libraryDependencies += "dev.zio" %% "zio-logging" % zioLoggingVersion exclude ("dev.zio", "zio"),
      libraryDependencies += "dev.zio" %% "zio-aws-netty" % zioAwsVersion exclude ("dev.zio", "zio"),
      libraryDependencies += "dev.zio" %% "zio-aws-sqs" % zioAwsVersion exclude ("dev.zio", "zio"),
      libraryDependencies += "dev.zio" %% "zio" % zioVersion,
      libraryDependencies += "dev.zio" %% "zio-json" % zioJsonVersion exclude ("dev.zio", "zio")
    )

lazy val irc =
  project
    .in(file("modules/irc"))
    .settings(
      moduleName := "irc",
      resolvers += "jitpack" at "https://jitpack.io/", // needed for pircbotx
      libraryDependencies += "com.github.pircbotx" % "pircbotx" % "2.2",
      assembly / mainClass := Some("sectery.irc.Main"),
      assembly / assemblyJarName := s"${name.value}.jar",
      Compile / run / fork := true
    )
    .dependsOn(shared)

lazy val producers =
  project
    .in(file("modules/producers"))
    .enablePlugins(BuildInfoPlugin)
    .settings(
      libraryDependencies += "com.h2database" % "h2" % "2.1.214" % "test",
      libraryDependencies += "dev.zio" %% "zio-test" % zioVersion % "test",
      libraryDependencies += "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.33",
      libraryDependencies += "org.jsoup" % "jsoup" % "1.16.1",
      libraryDependencies += "org.ocpsoft.prettytime" % "prettytime" % "5.0.6.Final",
      testFrameworks += new TestFramework(
        "zio.test.sbt.ZTestFramework"
      ),
      Test / fork := true,
      Test / envVars :=
        Map(
          "AIRNOW_API_KEY" -> "alligator3",
          "OPEN_WEATHER_MAP_API_KEY" -> "alligator3",
          "FINNHUB_API_TOKEN" -> "alligator3"
        ),
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "sectery",
      assembly / mainClass := Some("sectery.producers.Main"),
      assembly / assemblyJarName := s"${name.value}.jar",
      moduleName := "producers",
      Compile / run / fork := true
    )
    .dependsOn(shared)

lazy val aggregator =
  project
    .in(file("."))
    .aggregate(shared, irc, producers)
