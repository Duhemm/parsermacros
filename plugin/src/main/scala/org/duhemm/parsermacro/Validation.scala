package org.duhemm.parsermacro

trait Validation extends Signatures { self: Plugin =>
  import global._
  import analyzer._
  import definitions._
  import scala.reflect.internal.Flags._

  /**
   * Determines whether a `DefDef` uses the new scala.meta macro syntax
   */
  private def isLightweightSyntax(tree: Tree): Boolean = tree match {
    case _: Ident        => false
    case _: Select       => false
    case TypeApply(t, _) => isLightweightSyntax(t)
    case _               => true
  }

  /**
   * Extractor for parser macro implementations. They have to use the scala.meta macro syntax:
   *   def foo: Any = macro { ... }
   */
  object ParserMacroImpl {
    def unapply(tree: Tree): Option[(Modifiers, TermName, List[TypeDef], List[List[ValDef]], Tree, Tree)] = tree match {
      case DefDef(mods, name, tparams, vparamss, tpt, rhs) if isLightweightSyntax(rhs) && mods.hasFlag(MACRO) =>
        Some((mods, name, tparams, vparamss, tpt, rhs))

      case _ =>
        None
    }
  }

  /**
   * Verifies that the macro definition `ddef` is correct and can be used as a parser macro.
   * In case of success, attaches a macro impl binding to the macro def symbol's.
   */
  def verifyMacroShape(typer: Typer, ddef: DefDef): Option[Tree] = {

    ddef match {
      case ParserMacroImpl(_, _, _, _, _, _) =>
        val q"{ ${synthesizedImpl: DefDef}; () }" = typer.typed(q"{ ${synthesizeMacroImpl(ddef)}; () }")
        if (verifyMacroImpl(synthesizedImpl)) {
          ddef.symbol.addAnnotation(MacroImplAnnotation, ParserMacroSignature(synthesizedImpl))
          ddef.symbol.addAnnotation(MacroImplAnnotation, LegacySignature())
          Some(EmptyTree)
        } else None

      case _ =>
        None
    }

  }

  /**
   * Synthesizes a new macro implementation from the given `DefDef`.
   * This macro implementation will replace the original one (it will be removed during compilation).
   * We remove the MACRO modifier, because otherwise we will synthesize a new impl during typechecking
   * of this synthetic node, and so on.
   */
  def synthesizeMacroImpl(ddef: DefDef): DefDef = {
    val ParserMacroImpl(mods, name, tparams, vparamss, tpt, rhs) = ddef
    val newMods = mods &~ MACRO // The synthesized impl is not a macro
    atPos(ddef.pos)(q"$newMods def $name[..$tparams](...$vparamss): $tpt = $rhs")
  }

  /**
   * Verifies that all the formal parameters in `params` could be parameters of a parser macro.
   * This means that their types have to be supertypes of `Seq[scala.meta.Token]`.
   */
  private def parserMacroCompatibleParameters(params: List[Symbol]): Boolean = {
    val expectedType = typeOf[_root_.scala.meta.tokens.Tokens]
    params forall (expectedType <:< _.typeSignature)
  }

  /**
   * Verifies that `typedDDef` is a valid parser macro implementation.
   */
  private def verifyMacroImpl(typedDDef: DefDef): Boolean = {

    val sym = typedDDef.symbol.asMethod
    val params = sym.paramLists

    // We check whether all the parameters of this macro could be parser macro arguments. This
    // gives us a pretty reliable heuristic to know whether someone is trying to declare a parser macro.
    if (params forall parserMacroCompatibleParameters) {

      if (params.length != 1) {
        throw InvalidMacroShapeException(sym.pos, "parser macro can have only one parameter list.")
      }

      if (params.head exists (_.isImplicit)) {
        throw InvalidMacroShapeException(sym.pos, "parser macro cannot have implicit parameters.")
      }

      if (!(sym.returnType <:< typeOf[_root_.scala.meta.Tree])) {
        throw InvalidMacroShapeException(sym.pos, "macro implementation must return a value of type scala.meta.Tree.")
      }

      if (!sym.isPublic) {
        throw InvalidMacroShapeException(sym.pos, "macro implementation must be public.")
      }

      if (!typedDDef.symbol.owner.isStatic) {
        throw InvalidMacroShapeException(typedDDef.pos,
          """macro implementation reference has wrong shape. Required:
            |macro [<static object>].<method name>""".stripMargin)
      }

      true

    } else false
  }

}
