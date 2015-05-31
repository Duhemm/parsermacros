package macros
package deeply.nested.pkg

import scala.meta._

object DeeplyNestedParserMacro {
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
