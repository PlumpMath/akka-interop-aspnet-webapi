name := "akka-interop.client"
version := "1.0"

scalaVersion := "2.11.7"
val scalaDependencies = Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.7",
  "org.scala-lang.modules" %% "scala-async" % "0.9.5"
)
libraryDependencies ++= scalaDependencies

val akkaVersion = "2.3.9"
val akkaStreamsVersion = "1.0"
val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamsVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamsVersion,
  "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamsVersion
)
libraryDependencies ++= akkaDependencies

libraryDependencies += "com.lambdaworks" %% "jacks" % "2.3.3"
