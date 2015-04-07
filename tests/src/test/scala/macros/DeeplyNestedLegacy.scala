package macros
package deeply.nested.pkg

import scala.meta._

object DeeplyNestedLegacy {
  object Deeply {
    object Nested {
      object Obj {
        def impl(t: Seq[Token]) = internal.ast.Lit.Int(1)
        def foo = macro impl
      }
    }
  }
}
