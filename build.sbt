val scala3Version = "3.0.0"
val zioVersion = "1.0.8"

lazy val root = project
  .in(file("."))
  .settings(
    name := "sectery",
    scalaVersion := scala3Version,
    libraryDependencies += "dev.zio" %% "zio" % zioVersion,
    libraryDependencies += "dev.zio" %% "zio" % zioVersion,
    libraryDependencies += "dev.zio" %% "zio-test" % zioVersion % "test",
    libraryDependencies += "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
