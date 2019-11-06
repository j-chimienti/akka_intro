name := "akka_intro"

version := "0.1"

scalaVersion := "2.13.1"

val akkaVersion = "2.5.26"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
   "org.scalactic" %% "scalactic" % "3.0.8",
    "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)
