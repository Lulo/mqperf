name := "mqperf"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk" % "1.9.25" exclude("commons-logging", "commons-logging"),
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.7",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
  "org.scalatest" %% "scalatest" % "2.1.3" % "test"
)