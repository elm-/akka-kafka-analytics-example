import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.SettingsHelper._
import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport.Debian
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker

val shortCommit = ("git rev-parse --short HEAD" !!).replaceAll("\\n", "").replaceAll("\\r", "")


lazy val commonSettings = Seq(
  organization := "org.elmarweber.github",
  version := s"1.0.0-${shortCommit}",
  scalaVersion := "2.11.8",
  resolvers += Resolver.jcenterRepo,

  scalacOptions := Seq(
    "-encoding", "utf8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-target:jvm-1.7",
    "-Xlog-reflective-calls",
    "-Ypatmat-exhaust-depth", "40",
    "-Xmax-classfile-name", "240", // for docker container
  //      "-Xlog-implicits",
  //      disable compiler switches for now, some of them make an issue with recompilations
    "-optimise"
  //      "-Yclosure-elim",
  //      "-Yinline",
  //      "-Ybackend:GenBCode"
  ),

  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { x => false },
  publishArtifact in (Compile, packageDoc) := false,

  updateOptions := updateOptions.value.withCachedResolution(true)
)


val commonDockerSettings = Seq(
  packageName in Docker := "cpy-docker-test/" + name.value,
  version in Docker     := shortCommit,
  dockerBaseImage       := "airdock/oracle-jdk:jdk-1.8",
  defaultLinuxInstallLocation in Docker := s"/opt/${name.value}", // to have consistent directory for files
  dockerRepository := Some("eu.gcr.io"),

  //Debian packaging
  maintainer := "Cupenya Support <support@cupenya.com>"
) ++ makeDeploymentSettings(Debian, packageBin in Debian, "deb")

val defaultLib = Seq(
  libraryDependencies ++= {
    val akkaV            = "2.5.3"
    val akkaHttpV        = "10.0.9"
    val slf4sV           = "1.7.10"
    val logbackV         = "1.1.3"
    val scalaCommonV     = "2.4.1"
    val kamonVersion     = "0.6.7"
    val specs2Version    = "3.8.6"
    val scaffeineVersion = "2.1.0"
    val akkaStreamKafkaV = "0.17"
    Seq(
      "com.typesafe.akka" %% "akka-http"                         % akkaHttpV,
      "com.typesafe.akka" %% "akka-http-spray-json"              % akkaHttpV,
      "com.typesafe.akka" %% "akka-slf4j"                        % akkaV,
      "com.typesafe.akka" %% "akka-stream"                       % akkaV,
      "com.typesafe.akka" %% "akka-actor"                        % akkaV,
      "com.typesafe.akka" %% "akka-stream-kafka"                 % akkaStreamKafkaV,
      "io.kamon"          %% "kamon-core"                        % kamonVersion,
      "io.kamon"          %% "kamon-scala"                       % kamonVersion,
      "io.kamon"          %% "kamon-akka-2.5"                    % kamonVersion,
      "io.kamon"          %% "kamon-system-metrics"              % kamonVersion,
      "io.kamon"          %% "kamon-datadog"                     % kamonVersion,
      "org.slf4s"         %% "slf4s-api"                         % slf4sV,
      "ch.qos.logback"     % "logback-classic"                   % logbackV,
      "com.github.blemale" %% "scaffeine"                        % scaffeineVersion,
      "com.typesafe.scala-logging" %% "scala-logging"            % "3.7.2",
      "net.manub"         %% "scalatest-embedded-kafka"          % "0.15.1" % Test,
      "org.slf4j"         %  "log4j-over-slf4j"                  % "1.7.21" % Test,
      "org.specs2"        %% "specs2-core"                       % specs2Version    % Test,
      "org.specs2"        %% "specs2-mock"                       % specs2Version    % Test,
      "com.typesafe.akka" %% "akka-http-testkit"                 % akkaHttpV        % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"               % akkaV            % Test
    )
  }
)

lazy val root = (project in file("."))
  .settings(Seq(name := "akka-kafka-analytics-example"))
  .settings(commonSettings)
  .settings(commonDockerSettings)
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JDebPackaging)
  .enablePlugins(JavaAgent)
  .settings(Seq(javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.10"))
  .settings(defaultLib)


