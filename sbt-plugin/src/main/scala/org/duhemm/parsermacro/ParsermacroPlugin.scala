package org.duhemm.parsermacro

import sbt._
import Keys._

object ParsermacroPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings = Seq(
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    resolvers += Resolver.bintrayRepo("duhemm", "parsermacros"),
    libraryDependencies += "org.scalameta" % "scalameta_2.11" % "1.7.0",
    addCompilerPlugin("org.duhemm" % "plugin_2.11" % "0.1.1-SNAPSHOT" intransitive())
  )

}
