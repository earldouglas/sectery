val zioVersion = "2.0.20"
val zioJsonVersion = "0.6.2"
val zioLoggingVersion = "2.1.16"

ThisBuild / scalaVersion := "3.3.1"

ThisBuild / assembly / assemblyMergeStrategy := {
  case "module-info.class"                     => MergeStrategy.first
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case "META-INF/versions/9/module-info.class" => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val shared =
  project
    .in(file("modules/shared"))
    .settings(
      libraryDependencies += "dev.zio" %% "zio-logging" % zioLoggingVersion exclude ("dev.zio", "zio"),
      libraryDependencies += "dev.zio" %% "zio" % zioVersion,
      libraryDependencies += "dev.zio" %% "zio-json" % zioJsonVersion exclude ("dev.zio", "zio"),
      libraryDependencies += "com.rabbitmq" % "amqp-client" % "5.20.0"
    )

lazy val irc =
  project
    .in(file("modules/irc"))
    .settings(
      moduleName := "irc",
      resolvers += "jitpack" at "https://jitpack.io/", // needed for pircbotx
      libraryDependencies += "com.github.pircbotx" % "pircbotx" % "2.3.1",
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
      libraryDependencies += "com.h2database" % "h2" % "2.2.224" % "test",
      libraryDependencies += "dev.zio" %% "zio-test" % zioVersion % "test",
      libraryDependencies += "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      libraryDependencies += "org.mariadb.jdbc" % "mariadb-java-client" % "3.3.2",
      libraryDependencies += "org.jsoup" % "jsoup" % "1.17.2",
      libraryDependencies += "org.ocpsoft.prettytime" % "prettytime" % "5.0.7.Final",
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
