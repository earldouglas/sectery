val zioVersion = "2.1.19"
val zioJsonVersion = "0.7.44"
val zioLoggingVersion = "2.5.0"
val testcontainersVersion = "0.43.0"

ThisBuild / scalaVersion := "3.7.1"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

ThisBuild / assembly / assemblyMergeStrategy := {
  case "module-info.class"                     => MergeStrategy.first
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case "META-INF/versions/9/module-info.class" => MergeStrategy.first
  case "META-INF/okio.kotlin_module"           => MergeStrategy.first
  case "logback.xml"                           => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val domain =
  project
    .in(file("modules/1-domain"))

lazy val effects =
  project
    .in(file("modules/2-effects"))
    .dependsOn(domain)
    .settings(
      libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test
    )

lazy val use_cases =
  project
    .in(file("modules/3-use-cases"))
    .enablePlugins(BuildInfoPlugin)
    .settings(
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "sectery.usecases",
      libraryDependencies += "org.jsoup" % "jsoup" % "1.20.1",
      libraryDependencies += "org.ocpsoft.prettytime" % "prettytime" % "5.0.9.Final",
      libraryDependencies += "net.objecthunter" % "exp4j" % "0.4.8",
      libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test,
      libraryDependencies += "org.scalameta" %% "munit-scalacheck" % "1.0.0" % Test
    )
    .dependsOn(domain, effects)

lazy val adaptors =
  project
    .in(file("modules/4-adaptors"))
    .enablePlugins(BuildInfoPlugin)
    .settings(
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "sectery.adaptors",
      libraryDependencies += "dev.zio" %% "zio-json" % zioJsonVersion exclude (
        "dev.zio",
        "zio"
      ),
      libraryDependencies += "com.h2database" % "h2" % "2.3.232" % Test,
      libraryDependencies += "org.scalameta" %% "munit" % "1.1.1" % Test
    )
    .dependsOn(effects)

lazy val adaptors_with_zio =
  project
    .in(file("modules/4-adaptors-with-zio"))
    .settings(
      libraryDependencies += "com.rabbitmq" % "amqp-client" % "5.25.0",
      libraryDependencies += "dev.zio" %% "zio" % zioVersion,
      libraryDependencies += "dev.zio" %% "zio-json" % zioJsonVersion exclude (
        "dev.zio",
        "zio"
      )
    )
    .dependsOn(domain, effects, use_cases)

lazy val producers =
  project
    .in(file("modules/5-producers"))
    .settings(
      moduleName := "producers",
      libraryDependencies += "org.mariadb.jdbc" % "mariadb-java-client" % "3.5.3",
      libraryDependencies += "net.java.dev.jna" % "jna" % "5.17.0",
      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.18",
      libraryDependencies += "dev.zio" %% "zio-logging" % zioLoggingVersion exclude (
        "dev.zio",
        "zio"
      ),
      libraryDependencies += "dev.zio" %% "zio-logging-slf4j2" % zioLoggingVersion exclude (
        "dev.zio",
        "zio"
      ),
      assembly / mainClass := Some("sectery.producers.Main"),
      assembly / assemblyJarName := s"${name.value}.jar",
      Compile / run / fork := true
    )
    .dependsOn(adaptors_with_zio, use_cases, adaptors)

lazy val irc =
  project
    .in(file("modules/5-irc"))
    .settings(
      moduleName := "irc",
      resolvers += "jitpack" at "https://jitpack.io/", // needed for pircbotx
      libraryDependencies += "com.github.pircbotx" % "pircbotx" % "2.3.1",
      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.18",
      libraryDependencies += "dev.zio" %% "zio-logging" % zioLoggingVersion exclude (
        "dev.zio",
        "zio"
      ),
      libraryDependencies += "dev.zio" %% "zio-logging-slf4j2" % zioLoggingVersion exclude (
        "dev.zio",
        "zio"
      ),
      assembly / mainClass := Some("sectery.irc.Main"),
      assembly / assemblyJarName := s"${name.value}.jar",
      Compile / run / fork := true
    )
    .dependsOn(adaptors_with_zio, use_cases, adaptors)

lazy val slack =
  project
    .in(file("modules/5-slack"))
    .settings(
      moduleName := "slack",
      libraryDependencies += "com.slack.api" % "bolt-socket-mode" % "1.45.3",
      libraryDependencies += "javax.websocket" % "javax.websocket-api" % "1.1" % Provided,
      libraryDependencies += "org.glassfish.tyrus.bundles" % "tyrus-standalone-client" % "2.2.0",
      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.18",
      libraryDependencies += "dev.zio" %% "zio-logging" % zioLoggingVersion exclude (
        "dev.zio",
        "zio"
      ),
      libraryDependencies += "dev.zio" %% "zio-logging-slf4j2" % zioLoggingVersion exclude (
        "dev.zio",
        "zio"
      ),
      assembly / mainClass := Some("sectery.slack.Main"),
      assembly / assemblyJarName := s"${name.value}.jar",
      Compile / run / fork := true
    )
    .dependsOn(adaptors_with_zio, use_cases, adaptors)

lazy val root =
  project
    .in(file("."))
    .settings(
      libraryDependencies += "com.dimafeng" %% "testcontainers-scala-rabbitmq" % testcontainersVersion % Test,
      libraryDependencies += "com.dimafeng" %% "testcontainers-scala-mariadb" % testcontainersVersion % Test,
      Test / run / fork := true
    )
    .dependsOn(producers, irc, slack)
    .aggregate(
      producers,
      irc,
      slack,
      adaptors_with_zio,
      adaptors,
      use_cases,
      effects,
      domain
    )
