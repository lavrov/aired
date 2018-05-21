import sbt._, Keys._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

object SparkProject {
  val Deps = Seq(
    "org.apache.spark" %% "spark-sql" % "2.2.0",
    "org.apache.spark" %% "spark-streaming" % "2.2.0",
    "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.2.0",
    "com.datastax.spark" %% "spark-cassandra-connector" % "2.0.6",
    "com.holdenkarau" %% "spark-testing-base" % "2.2.0_0.7.2" % "test"
  )

  val Settings = Seq(
    parallelExecution in Test := false,
    fork := true,
    Universal / javaOptions ++= Seq("-J-Xms512M", "-J-Xmx2048M", "-Dspark.master=local[*]"),
    libraryDependencies ++= Deps
  )

}
