package macros

import scala.meta._
import scala.meta.dialects.Scala211

object LegacyWhitebox {

  // For these first tests the name of the implementation HAS to be
  // impl (extraction of the macro impl binding has not yet been implemented
  // for whitebox macros).
  def addMethodNamed = macro impl
  def impl(t: Seq[Token]): Tree = {
    val name = internal.ast.Term.Name(t(1).code)
    q"def $name: Unit = ()"
  }

}
