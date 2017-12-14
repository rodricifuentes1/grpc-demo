import sbt._

// -----------------------------------------------------
// Build information for project authentication
// -----------------------------------------------------
organization := "com.example"
name := "grpc-demo"

// -----------------------------------------------------
// Scala compiler configurations
// -----------------------------------------------------
scalaVersion := "2.12.4"
scalacOptions ++= Seq("-target:jvm-1.8")

// -----------------------------------------------------
// Resolvers and dependencies
// -----------------------------------------------------
libraryDependencies ++= Seq(
  "ch.qos.logback"             % "logback-classic"       % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging"        % "3.7.2",
  "com.trueaccord.scalapb"     %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion,
  "io.grpc"                    % "grpc-netty"            % com.trueaccord.scalapb.compiler.Version.grpcJavaVersion,
  "io.monix"                   %% "monix"                % "2.3.2",
  "org.scalatest"              %% "scalatest"            % "3.0.4" % Test
)

PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)

// -----------------------------------------------------
// Projects definitions
// -----------------------------------------------------
lazy val root: Project = Project("grpc-demo", file("."))
  .enablePlugins(JavaAppPackaging)

// -----------------------------------------------------
// Aliases
// -----------------------------------------------------
addCommandAlias("compile", ";scalafmt;compile:compile")
addCommandAlias("validate", ";scalafmtTest;clean;test")
