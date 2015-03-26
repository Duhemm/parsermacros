package org.duhemm.parsermacro

import scala.tools.nsc.interpreter.{ ReplGlobal => NscReplGlobal, _ }

// Stolen from scalameta/scalahost
trait ReplGlobal extends NscReplGlobal { self =>
  // TODO: classloader happy meal!!
  // can't cast analyzer to PalladiumSyntaxAnalyzer and use newUnitScanner/newUnitParser because of a classloader mismatch :O
  import syntaxAnalyzer.{ UnitScanner, UnitParser }
  override def newUnitScanner(unit: CompilationUnit): UnitScanner = {
    val m_newUnitScanner = syntaxAnalyzer.getClass.getMethods.filter(_.getName == "newUnitScanner").head
    m_newUnitScanner.invoke(syntaxAnalyzer, unit).asInstanceOf[UnitScanner]
  }
  override def newUnitParser(unit: CompilationUnit): UnitParser = {
    val m_newUnitParser = syntaxAnalyzer.getClass.getMethods.filter(_.getName == "newUnitParser").head
    m_newUnitParser.invoke(syntaxAnalyzer, unit).asInstanceOf[UnitParser]
  }
}
