package macros
package deeply.nested.pkg

import scala.meta._

object DeeplyNestedParserMacro {
  object Deeply {
    object Nested {
      object Obj {
        def foo(t: Tokens) = macro {
          scala.meta.Lit(1)
        }
      }
    }
  }
}
