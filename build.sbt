name := "magic-akka"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.6.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "org.jsoup" % "jsoup" % "1.12.2",
  "org.apache.commons" % "commons-text" % "1.8",
  "org.json4s" %% "json4s-native" % "3.6.7",
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.0.2",
  "com.lightbend.akka" %% "akka-stream-alpakka-mongodb" % "2.0.0-RC2",
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha5"
)