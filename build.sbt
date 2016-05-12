import com.typesafe.sbt.SbtAspectj._

val kamonVersion = "0.6.1"

lazy val root = (project in file("."))
  .settings(
    name := "kamon-akka-stream",
    version := "0.1",
    scalaVersion := "2.11.8",
    resolvers += "spray repo" at "http://repo.spray.io",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % "2.4.4",
      "io.kamon" %% "kamon-core" % kamonVersion,
      "io.kamon" %% "kamon-system-metrics" % kamonVersion,
      "io.kamon" %% "kamon-scala" % kamonVersion,
      "io.kamon" %% "kamon-akka" % kamonVersion,
      "io.kamon" %% "kamon-log-reporter" % kamonVersion
    ),
    aspectjSettings,
    javaOptions in run <++= AspectjKeys.weaverOptions in Aspectj,
    fork in run := true
  )
