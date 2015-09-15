package org.duhemm.parsermacro

import sbt._
import Keys._

object ParsermacroPlugin extends AutoPlugin {

  object autoImport {

    lazy val parsermacroSettings: Seq[Def.Setting[_]] = Seq(
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      libraryDependencies += "org.scalameta" % "scalameta_2.11" % "0.1.0-SNAPSHOT",
      addCompilerPlugin("org.duhemm" % "plugin_2.11" % "0.1.0-SNAPSHOT" intransitive())
    )
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings =
    parsermacroSettings

}
