addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")