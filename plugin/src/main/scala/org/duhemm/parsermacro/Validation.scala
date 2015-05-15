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
   * Extractor for parser macro implementations that use the scala.reflect macro syntax:
   *   def impl(tokens: Seq[Token]): Tree = ...
   *   def foo: Any = macro impl
   */
  object LegacyParserMacroImpl {
    def unapply(tree: Tree): Option[(Modifiers, TermName, List[TypeDef], List[List[ValDef]], Tree, Tree)] = tree match {
      case DefDef(mods, name, tparams, vparamss, tpt, rhs) if !isLightweightSyntax(rhs) =>
        Some((mods, name, tparams, vparamss, tpt, rhs))

      case _ =>
        None
    }
  }

  /**
   * Extractor for parser macro implemenations that ue the scala.meta macro syntax:
   *   def foo: Any = macro { ... }
   */
  object LightweightParserMacroImpl {
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
      case LegacyParserMacroImpl(mods, name, tparams, vparamss, tpt, rhs) =>
        typer.silent(_.typed(markMacroImplRef(rhs))) map {
          case select: Select if verifyLegacyMacroImpl(select) =>
            bindMacroImpl(ddef.symbol, select)
            Some(select)

          case TypeApply(select: Select, _) if verifyLegacyMacroImpl(select) =>
            bindMacroImpl(ddef.symbol, select)
            Some(select)

          case _ =>
            None

        } orElse {
          case err +: _ => throw InvalidMacroShapeException(err.errPos, err.errMsg)
        }

      case LightweightParserMacroImpl(_, _, _, _, _, _) =>
        val q"{ ${synthesizedImpl: DefDef}; () }" = typer.typed(q"{ ${synthesizeMacroImpl(ddef)}; () }")
        if (verifyScalaMetaMacroImpl(synthesizedImpl)) {
          ddef.symbol.addAnnotation(MacroImplAnnotation, ScalahostSignature(synthesizedImpl))
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
    val LightweightParserMacroImpl(mods, name, tparams, vparamss, tpt, rhs) = ddef
    val newMods = mods &~ MACRO // The synthesized impl is not a macro
    atPos(ddef.pos)(q"$newMods def $name[..$tparams](...$vparamss): $tpt = $rhs")
  }

  /**
   * Verifies that all the formal parameters in `params` could be parameters of a parser macro.
   * This means that their types have to be supertypes of `Seq[scala.meta.Token]`.
   */
  private def parserMacroCompatibleParameters(params: List[Symbol]): Boolean = {
    val expectedType = typeOf[_root_.scala.collection.Seq[_root_.scala.meta.syntactic.Token]]
    params forall (expectedType <:< _.typeSignature)
  }

  /**
   * Verifies that the (typed) right hand side of a macro def (old syntax) is a valid parser macro
   */
  private def verifyLegacyMacroImpl(typedSelect: Select): Boolean = {
    val sym = typedSelect.symbol

    if (!sym.isMethod) {
      false
    } else {
      val macroImplSym = sym.asMethod

      if (verifyMacroImpl(macroImplSym)) {

        if (!macroImplSym.isStatic && !typedSelect.qualifier.symbol.isStatic) {
          throw InvalidMacroShapeException(typedSelect.pos,
            """macro implementation reference has wrong shape. Required:
              |macro [<static object>].<method name>""".stripMargin)
        }

        true

      } else false

    }
  }

  /**
   * Verifies that the (typed) `DefDef` (new syntax) can be used as a valid parser macro
   */
  private def verifyScalaMetaMacroImpl(typedDDef: DefDef): Boolean = {
    val sym = typedDDef.symbol

    if (!sym.isMethod) {
      false
    } else {
      val macroImplSym = sym.asMethod

      if (verifyMacroImpl(macroImplSym)) {

        if (!typedDDef.symbol.owner.isStatic) {
          throw InvalidMacroShapeException(typedDDef.pos,
            """macro implementation reference has wrong shape. Required:
              |macro [<static object>].<method name>""".stripMargin)
        }

        true

      } else false
    }
  }

  /**
   * Verifies that `sym` represents a valid parser macro.
   */
  private def verifyMacroImpl(sym: MethodSymbol): Boolean = {
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

      true

    } else false
  }


}
