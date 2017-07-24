val scalaHomeProperty = "macroparser.scala.home"

lazy val bintraySettings: Seq[Setting[_]] = Seq(
  bintrayOrganization := Some("duhemm"),
  bintrayRepository := "parsermacros",
  bintrayVcsUrl := Some("git@github.com:Duhemm/parsermacros.git"),
  licenses += ("BSD", url("https://github.com/Duhemm/parsermacros/blob/master/LICENSE"))
)

lazy val sharedSettings: Seq[Setting[_]] = Seq(
  version := "0.1.1-SNAPSHOT",
  scalaVersion := "2.11.8",
  organization := "org.duhemm",
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  libraryDependencies += "org.scalameta" %% "scalameta" % "1.6.0",
  libraryDependencies += (scalaVersion)("org.scala-lang" % "scala-reflect" % _).value,
  scalaHome := sys.props get scalaHomeProperty map file
) ++ bintraySettings

lazy val testSettings: Seq[Setting[_]] = Seq(
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
  // We depend on Macro Paradise, because we need its JAR on the classpath to give it to
  // the test compiler.
  libraryDependencies += "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full,
  fullClasspath in Test := {
    val testcp = (fullClasspath in Test).value.files.map(_.getAbsolutePath).mkString(java.io.File.pathSeparatorChar.toString)
    sys.props("sbt.class.directory") = testcp

    val paradise = (fullClasspath in Test).value.files find (_.getName contains "paradise") map (_.getAbsolutePath) getOrElse ""
    sys.props("sbt.path.paradise.jar") = paradise
    (fullClasspath in Test).value
  }
)

val pluginJarName = "fat-plugin.jar"

lazy val usePluginSettings = Seq(
  scalacOptions in Compile ++= ((Keys.`package` in (plugin, Compile)) map { (jar: File) =>
    val fatJar = file(jar.getParent + "/" + pluginJarName)
    System.setProperty("sbt.paths.plugin.jar", fatJar.getAbsolutePath)
    val addPlugin = "-Xplugin:" + fatJar.getAbsolutePath
    // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + fatJar.lastModified
    Seq(addPlugin, dummy)
  }).value
)

val duplicatedFiles = Set(
  // scalahost also provides `scalac-plugin.xml`, but we are only interested in ours.
  "scalac-plugin.xml",
  ".class"
 )

lazy val plugin: Project =
  (project in file("plugin")) settings (
    sharedSettings: _*
  ) settings (
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    libraryDependencies +=
      (scalaVersion)("org.scala-lang" % "scala-compiler" % _ % "provided").value,
    libraryDependencies +=
      "org.scalameta" % "scalahost" % "1.6.0" cross CrossVersion.full,
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, includeDependency = true),
    assemblyJarName in assembly := pluginJarName,
    assemblyMergeStrategy in assembly := {
      case x if duplicatedFiles exists (x endsWith _) => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    // Produce a fat jar containing dependencies of the plugin after compilation. This is required because the plugin
    // depends on scala.meta, which must therefore be available when the plugin is run.
    // It looks like this task is defined in the wrong order (assembly and then compilation), but it seems to work fine.
    compile := ((compile in Compile) dependsOn assembly).value,
    resourceDirectory in Compile := baseDirectory(_ / "src" / "main" / "scala" / "org" / "duhemm" / "parsermacro" / "embedded").value,
    initialCommands in console := """
      import scala.meta._
      import scala.meta.dialects.Scala211
    """,
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, assembly) := true,
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.copy(`classifier` = None)
    },
    addArtifact(artifact in (Compile, assembly), assembly)
  )

lazy val sbtParsermacros: Project =
  (project in file("sbt-plugin")) settings (
    bintraySettings,
    organization := "org.duhemm",
    name := "sbt-parsermacros",
    sbtPlugin := true
  )

lazy val sandboxMacros: Project =
  (project in file("sandbox-macros")) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    publishArtifact in Compile := false,
    compile := (compile in Compile).value
  )

lazy val sandboxClients =
  (project in file("sandbox-clients")) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    // Always clean before running compile in this subproject
    compile := ((compile in Compile) dependsOn clean).value,
    scalacOptions ++= Seq("-Ymacro-debug-verbose")
  ) dependsOn sandboxMacros

lazy val tests =
  (project in file("tests")) settings (
    sharedSettings ++ usePluginSettings ++ testSettings: _*
  ) settings (
    libraryDependencies += (scalaVersion)("org.scala-lang" % "scala-compiler" % _).value
  ) dependsOn plugin
