ThisBuild / scalaVersion := "2.11.8"

lazy val aired = (project in file("."))
  .settings(
    inThisBuild(List(
      organization := "com.github.lavrov.aired",
      version := "0.0.1",
      Compile / run / fork := true,
    )))
  .aggregate(shared, importer, processor, `web-service`, ui)

lazy val shared = project
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.6.7",
      "org.scalatest" %% "scalatest" % "3.0.4"
    )
  )

lazy val importer = project
  .enablePlugins(JavaAppPackaging)
  .settings(
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.18",
      "com.typesafe.akka" %% "akka-http" % "10.1.0-RC1",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ),
    dockerIncludeWaitForIt
  )
  .dependsOn(shared)

lazy val processor = project
  .enablePlugins(JavaAppPackaging)
  .settings(SparkProject.Settings)
  .settings(
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      "com.github.lavrov.aired" %% "shared" % version.value,
      "com.fasterxml.jackson.core" % "jackson-databind" % "[2.6.5, 2.6.5]" force(),
      "com.typesafe" % "config" % "1.3.2"
    ),
    dockerIncludeWaitForIt
  )

lazy val `web-service` = project
  .enablePlugins(JavaAppPackaging)
  .settings(
    scalaVersion := "2.12.4",
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.1.0-RC1",
      "com.typesafe.akka" %% "akka-stream" % "2.5.8",
      "com.typesafe.akka" %% "akka-stream-kafka" % "0.18",
      "com.typesafe.akka" %% "akka-typed" % "2.5.8",
      "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.8",
      "com.typesafe.play" %% "play-json" % "2.6.7",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.softwaremill.macwire" %% "macros" % "2.3.0" % Provided,
      "com.softwaremill.macwire" %% "util" % "2.3.0",
      "io.getquill" %% "quill-cassandra" % "2.3.2",
    ),
    dockerIncludeWaitForIt
  )
  .dependsOn(shared)

lazy val ui = project
  .enablePlugins(ScalaJSPlugin, WorkbenchSplicePlugin)
  .settings(
    scalaVersion := "2.12.4",
    libraryDependencies += "com.thoughtworks.binding" %%% "dom" % "11.0.1",
    scalaJSUseMainModuleInitializer := true,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

def dockerIncludeWaitForIt = {
  import com.typesafe.sbt.packager.docker._
  Seq(
    dockerCommands :=
        dockerCommands.value.dropRight(2) ++
        Seq(
          Cmd("ADD", "--chown=daemon:daemon", "https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh ."),
          ExecCmd("RUN", "chmod", "a+x", "wait-for-it.sh")
        ) ++
        dockerCommands.value.takeRight(2)
  )
}
