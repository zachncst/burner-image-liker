enablePlugins(DockerPlugin)

name := """burner-image-liker"""

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

mainClass in (Compile, run) := Some("WebServer")

val finchVersion = "0.11.0-M4"
val circeVersion = "0.5.3"

libraryDependencies ++= Seq(
  "io.verizon.knobs" %% "core" % "3.12.27",
  "io.circe" %% "circe-core" %  circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.github.finagle" %% "finch-core" % finchVersion,
  "com.github.finagle" %% "finch-circe" % finchVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

resolvers ++= Seq(
	 Resolver.sonatypeRepo("snapshots")
)


dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("java")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}
