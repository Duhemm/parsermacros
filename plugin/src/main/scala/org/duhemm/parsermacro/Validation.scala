package org.duhemm.parsermacro

trait Validation { self: Plugin =>
  import global._

  /**
   * Verifies that all the formal parameters in `params` could be parameters of a parser macro.
   * This means that their types have to be supertypes of `Seq[scala.meta.Token]`.
   */
  private def parserMacroCompatibleParameters(params: List[Symbol]): Boolean = {
    val expectedType = typeOf[_root_.scala.collection.Seq[_root_.scala.meta.syntactic.Token]]
    params forall (expectedType <:< _.typeSignature)
  }

  private[parsermacro] def verifyMacroImpl(select: Select): Boolean = {

    val sym = select.symbol

    if (!sym.isMethod) {
      false
    } else {
      val macroImplSym = sym.asMethod
      val params = macroImplSym.paramLists

      // We check whether all the parameters of this macro could be parser macro arguments. This
      // gives us a pretty reliable heuristic to know whether someone is trying to declare a parser macro.
      if (params forall parserMacroCompatibleParameters) {

        if (params.length != 1) {
          throw InvalidMacroShapeException(select.pos, "macro parser can have only one parameter list.")
        }

        if (params.head exists (_.isImplicit)) {
          throw InvalidMacroShapeException(select.pos, "macro parser cannot have implicit parameters.")
        }

        if (!(macroImplSym.returnType <:< typeOf[_root_.scala.meta.Tree])) {
          throw InvalidMacroShapeException(select.pos, "macro implementation must return a value of type scala.meta.Tree.")
        }

        if (!macroImplSym.isPublic) {
          throw InvalidMacroShapeException(select.pos, "macro implementation must be public.")
        }

        if (!macroImplSym.isStatic && !select.qualifier.symbol.isStatic) {
          throw InvalidMacroShapeException(select.pos,
            """macro implementation reference has wrong shape. Required:
              |macro [<static object>].<method name>""".stripMargin)
        }

        true

      } else false
    }
  }
}
