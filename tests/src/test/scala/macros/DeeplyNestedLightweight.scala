package macros
package deeply.nested.pkg

import scala.meta._

object DeeplyNestedLightweight {
  object Deeply {
    object Nested {
      object Obj {
        def foo(t: Seq[Token]) = macro {
          internal.ast.Lit.Int(1)
        }
      }
    }
  }
}
