addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.2.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.3")

// fast turnaround / restart app
addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")
//Debian packaging
libraryDependencies += "org.vafer" % "jdeb" % "1.3" artifacts (Artifact("jdeb", "jar", "jar"))
