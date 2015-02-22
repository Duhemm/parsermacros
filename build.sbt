
lazy val verifyScalaHome = taskKey[Unit]("Makes sure `scalaHome` is set.")
val scalaHomeProperty = "macroparser.scala.home"

lazy val sharedSettings: Seq[Setting[_]] = Seq(
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.5",
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
    publishArtifact in Compile := false
  )

lazy val sandboxClients =
  (project in file("sandbox-clients")) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    // Always clean before running compile in this subproject
    compile <<= (compile in Compile) dependsOn (verifyScalaHome, clean),
    verifyScalaHome := {
      if((System getProperty scalaHomeProperty) == null) {
        val log = streams.value.log
        log.error("This plugin can only work properly with a custom version of scalac, because it uses a special syntax for the application of macro parsers.")
        log.error("A version of scalac that has been patched to support macro parsers can be found here: https://github.com/Duhemm/scala/tree/macroparser")
        log.error("Please pass the path to your scala home in the following system property: " + scalaHomeProperty)
        log.error("Compilation of clients of macro parsers will fail.")
      }
    }
  ) dependsOn sandboxMacros

lazy val tests =
  (project in file("tests")) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"
  )
