ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .enablePlugins(JmhPlugin)
  .settings(
    name := "Benchmark",
    //libraryDependencies += "org.openjdk.jmh" %% "jmh-core" % "1.36",
    //libraryDependencies += "org.openjdk.jmh" %% "jmh-generator-annprocess" % "1.36",
  )
