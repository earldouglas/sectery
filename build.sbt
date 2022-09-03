val zioVersion = "2.0.1"
val zioAwsVersion = "5.17.267.1"
val zioJsonVersion = "0.3.0-RC11"

ThisBuild / scalaVersion := "3.1.2"
ThisBuild / scalacOptions += "-deprecation"
ThisBuild / scalacOptions += "-Xfatal-warnings"

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
      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11",
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
      libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.30",
      libraryDependencies += "org.jsoup" % "jsoup" % "1.15.2",
      libraryDependencies += "org.ocpsoft.prettytime" % "prettytime" % "5.0.3.Final",
      testFrameworks += new TestFramework(
        "zio.test.sbt.ZTestFramework"
      ),
      Test / fork := true,
      Test / envVars :=
        Map(
          "AIRNOW_API_KEY" -> "alligator3",
          "DARK_SKY_API_KEY" -> "alligator3",
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
