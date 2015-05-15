package org.duhemm.parsermacro

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{ Plugin => NscPlugin }

class Plugin(val global: Global) extends NscPlugin
                                    with HijackSyntaxAnalyzer
                                    with AnalyzerPlugins
                                    with UniverseProvider {
  import global.analyzer

  val name = "parsermacro"
  val description = "Scala compiler plugin implementing parsermacros"
  val components = Nil
  val universe = global.rootMirror.universe

  hijackSyntaxAnalyzer()

  analyzer.addMacroPlugin(MacroPlugin)

}
