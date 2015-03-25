package org.duhemm.parsermacro

import scala.tools.nsc.plugins.{ Plugin => NscPlugin }
import scala.tools.nsc.{ Global, SubComponent, Settings }
import scala.tools.nsc.interpreter.{ IMain, ReplReporter }
import scala.reflect.internal.util.BatchSourceFile

import scala.collection.mutable

/**
 * Hijacks the syntax analyzer to allow us to use a custom parser.
 * Taken from scalameta/scalahost
 */
trait HijackSyntaxAnalyzer { self: NscPlugin =>

  def hijackSyntaxAnalyzer(): Unit = {
    val syntaxAnalyzer = new { val global: self.global.type = self.global } with ParserMacroSyntaxAnalyzer
    val syntaxAnalyzerField = classOf[Global].getDeclaredField("syntaxAnalyzer")
    syntaxAnalyzerField.setAccessible(true)
    syntaxAnalyzerField.set(global, syntaxAnalyzer)

    val phasesDescMapGetter = classOf[Global].getDeclaredMethod("phasesDescMap")
    val phasesDescMap = phasesDescMapGetter.invoke(global).asInstanceOf[mutable.Map[SubComponent, String]]
    val phasesSetMapGetter = classOf[Global].getDeclaredMethod("phasesSet")
    val phasesSet = phasesSetMapGetter.invoke(global).asInstanceOf[mutable.Set[SubComponent]]
    if (phasesSet.exists(_.phaseName == "parser")) { // `scalac -help` doesn't instantiate standard phases
      val oldParser = phasesSet.find(_.phaseName == "parser").head
      val newParser = syntaxAnalyzer
      phasesSet -= oldParser
      phasesSet += newParser
      val phasesDescMapGetter = classOf[Global].getDeclaredMethod("phasesDescMap")
      val phasesDescMap = phasesDescMapGetter.invoke(global).asInstanceOf[mutable.Map[SubComponent, String]]
      phasesDescMap(newParser) = "parse source into ASTs, accept parser macros, perform simple desugaring"
    }

    if (global.reporter.isInstanceOf[ReplReporter] && global.toString != "<hijacked global>") {
      def obtainField(cls: Class[_], name: String) = { val result = cls.getDeclaredField(name); result.setAccessible(true); result }
      def obtainMethod(cls: Class[_], name: String) = { val result = cls.getDeclaredMethods().filter(_.getName == name).head; result.setAccessible(true); result }
      val f_intp = obtainField(classOf[ReplReporter], "intp")
      val f_compiler = obtainField(classOf[IMain], "_compiler")
      val m_initSources = obtainMethod(classOf[IMain], "_initSources")

      val intp = f_intp.get(global.reporter).asInstanceOf[IMain]
      val settings = new Settings()
      intp.settings.copyInto(settings)
      settings.outputDirs setSingleOutput intp.replOutput.dir
      settings.exposeEmptyPackage.value = true
      val hijackedCompiler = new Global(settings, global.reporter) with ReplGlobal { override def toString: String = "<hijacked global>" }
      val initSources = m_initSources.invoke(intp).asInstanceOf[List[BatchSourceFile]]
      new hijackedCompiler.Run().compileSources(initSources)
      f_compiler.set(intp, hijackedCompiler)
    }
  }

}
