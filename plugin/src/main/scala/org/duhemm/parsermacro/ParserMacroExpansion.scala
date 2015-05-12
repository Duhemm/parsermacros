package org.duhemm.parsermacro

import scala.reflect.macros.whitebox.Context
import scala.reflect.runtime.ReflectionUtils
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.annotation.compileTimeOnly

import scala.meta.dialects.Scala211
import scala.meta.Input.String.{ apply => string2input }

import java.lang.reflect.Method

class ExpandParserMacro(val c: Context) {

  import c.universe._
  import Flag._

  def impl(annottees: c.Tree*): c.Tree = {

    assert(annottees.length == 1)
    val annottee = annottees.head

    val q"new $_($originalTree).macroTransform($_)" = c.macroApplication
    val q"val tokens = scala.collection.immutable.List(..$rawArguments)" = annottee
    val typechecked = c.typecheck(originalTree, mode = c.TYPEmode)
    val methodSymbol = typechecked.symbol.asMethod
    val arguments = rawArguments collect { case Literal(Constant(arg: String)) => arg } map string2input map (_.tokens)

    getMacroImplementation(methodSymbol) match {
      case Some((instance, method)) =>
        val expanded = method.invoke(instance, arguments: _*)

        // There are many trees toGTree cannot convert at the moment...
        c.parse(expanded.toString)

      case _ =>
        c.abort(c.macroApplication.pos, "Could not find macro implementation")
    }
  }

  // TODO: Extract the macro implementationg binding :)
  // TODO: Refactor this to use the stuff that already exist in trait Runtime
  def getMacroImplementation(sym: MethodSymbol): Option[(Object, Method)] = {
    val ownerObject = sym.owner.asClass
    val ownerName = ownerObject.fullName
    val methName  = "impl"
    val cl = this.getClass.getClassLoader

    val clss = Class.forName(ownerName, true, cl)
    clss.getDeclaredMethods find (_.getName == methName) map { m =>
      (ReflectionUtils.staticSingletonInstance(cl, ownerName), m)
    }
  }
}

@compileTimeOnly("enable macro paradise to expand macro annotations")
class ParserMacroExpansion(args: Any) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ExpandParserMacro.impl
}
