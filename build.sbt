val scalaHomeProperty = "macroparser.scala.home"
lazy val sharedSettings: Seq[Setting[_]] = Seq(
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.6",
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  libraryDependencies += "org.scalameta" %% "scalameta" % "0.1.0-SNAPSHOT",
  libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
  scalaHome := {
    System getProperty scalaHomeProperty match {
      case null =>
        None
      case scalaHome => Some(file(scalaHome))
    }
  }
)

val pluginJarName = "fat-plugin.jar"

lazy val usePluginSettings = Seq(
  scalacOptions in Compile <++= (Keys.`package` in (plugin, Compile)) map { (jar: File) =>
    val fatJar = file(jar.getParent + "/" + pluginJarName)
    System.setProperty("sbt.paths.plugin.jar", fatJar.getAbsolutePath)
    val addPlugin = "-Xplugin:" + fatJar.getAbsolutePath
    // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + fatJar.lastModified
    Seq(addPlugin, dummy)
  }
)

lazy val plugin: Project =
  (project in file("plugin")) settings (
    sharedSettings: _*
  ) settings (
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, includeDependency = true),
    assemblyJarName in assembly := pluginJarName,
    // Produce a fat jar containing dependencies of the plugin after compilation. This is required because the plugin
    // depends on scala.meta, which must therefore be available when the plugin is run.
    // It looks like this task is defined in the wrong order (assembly and then compilation), but it seems to work fine.
    compile <<= (compile in Compile) dependsOn assembly,
    resourceDirectory in Compile <<= baseDirectory(_ / "src" / "main" / "scala" / "org" / "duhemm" / "parsermacro" / "embedded")
  )

lazy val sandboxMacros: Project =
  (project in file("sandbox-macros")) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    publishArtifact in Compile := false,
    compile <<= (compile in Compile)
  )

lazy val sandboxClients =
  (project in file("sandbox-clients")) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    // Always clean before running compile in this subproject
    compile <<= (compile in Compile) dependsOn clean,
    scalacOptions ++= Seq("-Ymacro-debug-verbose")
  ) dependsOn sandboxMacros

lazy val tests =
  (project in file("tests")) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
    compile in Test := {
      sys.props("sbt.class.directory") = (classDirectory in Test).value.getAbsolutePath
      (compile in Test).value
    }
  ) dependsOn plugin
