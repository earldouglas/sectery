val zioVersion = "2.0.0-RC1"

ThisBuild / scalaVersion := "3.1.0"
ThisBuild / scalacOptions += "-deprecation"
ThisBuild / scalacOptions += "-Xfatal-warnings"

lazy val shared =
  project
    .in(file("modules/shared"))

lazy val irc =
  project
    .in(file("modules/irc"))
    .settings(
      resolvers += "jitpack" at "https://jitpack.io/", // needed for pircbotx
      libraryDependencies += "dev.zio" %% "zio" % zioVersion,
      libraryDependencies += "com.github.pircbotx" % "pircbotx" % "2.2"
    )
    .dependsOn(shared)

lazy val producers =
  project
    .in(file("modules/producers"))
    .enablePlugins(BuildInfoPlugin)
    .settings(
      libraryDependencies += "org.ocpsoft.prettytime" % "prettytime" % "5.0.2.Final",
      libraryDependencies += "org.postgresql" % "postgresql" % "42.3.1",
      libraryDependencies += "org.jsoup" % "jsoup" % "1.14.3",
      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.10",
      libraryDependencies += "dev.zio" %% "zio" % zioVersion,
      libraryDependencies += "dev.zio" %% "zio-json" % "0.3.0-RC2",
      libraryDependencies += "dev.zio" %% "zio-test" % zioVersion % "test",
      libraryDependencies += "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.36.0.3" % "test",
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
      buildInfoPackage := "sectery"
    )
    .dependsOn(shared)

lazy val sectery =
  project
    .in(file("."))
    .enablePlugins(JavaAppPackaging)
    .settings(
      moduleName := "sectery",
      Compile / run / fork := true
    )
    .dependsOn(shared)
    .dependsOn(irc)
    .dependsOn(producers)
    .aggregate(shared, irc, producers)
