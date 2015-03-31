package org.duhemm.parsermacro

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{ Plugin => NscPlugin }
import scala.meta.internal.hosts.scalac.PluginBase

class Plugin(val global: Global) extends PluginBase with HijackSyntaxAnalyzer with AnalyzerPlugins {
  import global.analyzer

  val name = "parsermacro"
  val description = "Scala compiler plugin implementing parsermacros"
  val components = Nil

  override val (newAnalyzer, oldAnalyzer) = hijackAnalyzer()
  ifNecessaryReenableMacroParadise(oldAnalyzer)

  hijackSyntaxAnalyzer()
  analyzer.addMacroPlugin(MacroPlugin)

}
