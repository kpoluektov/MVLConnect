name := "MVLConnect"

version := "0.1"

scalaVersion := "2.12.7"

//cleanFiles += managedDirectory.value
//managedDirectory := baseDirectory.value / "libs"
//retrieveManaged := true
//useCoursier := true // set explicitly for clarity

resolvers += Resolver.mavenLocal


libraryDependencies ++= {
  val akkaVer = "2.5.23"
  val kafkaVer = "2.5.0"
  Seq(
    "com.typesafe.akka" %% "akka-actor"               % akkaVer,
    "org.apache.kafka" % "connect-api"                % kafkaVer,
    "org.apache.kafka" % "connect-json"               % kafkaVer,
    "com.typesafe.slick" %% "slick"                   % "3.3.1",
    "ch.qos.logback" % "logback-classic"              % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.2",
    "com.oracle.database.jdbc" % "ojdbc8"             % "12.2.0.1",
    "org.scalatest" %% "scalatest" % "3.2.2"          % Test
  )
}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}