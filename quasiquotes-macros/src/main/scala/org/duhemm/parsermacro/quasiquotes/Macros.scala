package scala.meta
package parsermacro

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

import org.scalameta.annotations.QuasiquoteMacros

import org.duhemm.parsermacro.quasiquotes.TokenQuasiquoteLiftables

class quasiquote[T](qname: Symbol) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro TokenQuasiquoteMacros.impl
}

class TokenQuasiquoteMacros(override val c: Context) extends QuasiquoteMacros(c) with TokenQuasiquoteLiftables {
  val u = c.universe
  import c.universe._

  val XtensionQuasiquoteTerm = "shadow scala.meta quasiquotes"
  override val ReificationMacros = q"_root_.scala.meta.parsermacro.TokenReificationMacros"
}
