package macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object VanillaMacros {
  def impl(c: Context): c.Tree = {
    import c.universe._
    q"1"
  }

  def vanilla: Int = macro impl
}
