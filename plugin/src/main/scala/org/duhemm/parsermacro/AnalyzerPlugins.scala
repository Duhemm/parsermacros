package org.duhemm.parsermacro

import scala.reflect.macros.util.Traces
import scala.reflect.internal.util.ScalaClassLoader
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.runtime.ReflectionUtils
import scala.reflect.internal.Mode
import scala.reflect.internal.Flags
import scala.reflect.internal.Flags._

trait AnalyzerPlugins extends Traces
                      with Validation
                      with Runtime { self: Plugin =>
  import global._
  import analyzer._
  import definitions._
  import treeInfo._

  def globalSettings = global.settings

  object MacroPlugin extends analyzer.MacroPlugin {

    /**
     * Prepares a list of statements for being typechecked by performing domain-specific type-agnostic code synthesis.
     *
     * Trees passed into this method are going to be named, but not typed.
     * In particular, you can rely on the compiler having called `enterSym` on every stat prior to passing calling this method.
     *
     * Default implementation does nothing. Current approaches to code syntheses (generation of underlying fields
     * for getters/setters, creation of companion objects for case classes, etc) are too disparate and ad-hoc
     * to be treated uniformly, so I'm leaving this for future work.
     */
    override def pluginsEnterStats(typer: Typer, stats: List[Tree]): List[Tree] = stats flatMap {
      // Whenever we encounter a lightweight parser macro definition, we synthesize a private implementation that will
      // be executed whenever we expand the macro.
      case ddef @ LightweightParserMacroImpl(_, name, _, _, _, _) =>
        val synthesized = copyDefDef(synthesizeMacroImpl(ddef.asInstanceOf[DefDef]))(mods = Modifiers(PRIVATE), name = TermName(name + "$impl"))
        newNamer(typer.context) enterSym synthesized
        List(synthesized, ddef)

      case other =>
        List(other)
    }

    /**
     * Typechecks the right-hand side of a macro definition (which typically features
     * a mere reference to a macro implementation).
     *
     * Default implementation provided in `self.standardTypedMacroBody` makes sure that the rhs
     * resolves to a reference to a method in either a static object or a macro bundle,
     * verifies that the referred method is compatible with the macro def and upon success
     * attaches a macro impl binding to the macro def's symbol.
     *
     * $nonCumulativeReturnValueDoc.
     */
    override def pluginsTypedMacroBody(typer: Typer, ddef: DefDef): Option[Tree] = {
      try {
        verifyMacroShape(typer, ddef) map (_ => EmptyTree)
      } catch {
        case InvalidMacroShapeException(pos, msg) =>
          macroLogVerbose(s"parser macro $ddef.name has an invalid shape:\n$msg")
          typer.context.error(pos, msg)
          None
      }
    }

    /**
     * Figures out whether the given macro definition is blackbox or whitebox.
     *
     * Default implementation provided in `self.standardIsBlackbox` loads the macro impl binding
     * and fetches boxity from the "isBlackbox" field of the macro signature.
     *
     * $nonCumulativeReturnValueDoc.
     */
    override def pluginsIsBlackbox(macroDef: Symbol): Option[Boolean] = Some(true)

    /**
     * Expands an application of a def macro (i.e. of a symbol that has the MACRO flag set),
     * possibly using the current typer mode and the provided prototype.
     *
     * Default implementation provided in `self.standardMacroExpand` figures out whether the `expandee`
     * needs to be expanded right away or its expansion has to be delayed until all undetermined
     * parameters are inferred, then loads the macro implementation using `self.pluginsMacroRuntime`,
     * prepares the invocation arguments for the macro implementation using `self.pluginsMacroArgs`,
     * and finally calls into the macro implementation. After the call returns, it typechecks
     * the expansion and performs some bookkeeping.
     *
     * This method is typically implemented if your plugin requires significant changes to the macro engine.
     * If you only need to customize the macro context, consider implementing `pluginsMacroArgs`.
     * If you only need to customize how macro implementation are invoked, consider going for `pluginsMacroRuntime`.
     *
     * $nonCumulativeReturnValueDoc.
     */
    override def pluginsMacroExpand(typer: Typer, expandee: Tree, mode: Mode, pt: Type): Option[Tree] = {

      expandee match {
        case ParserMacroApplication(app, rawArguments, _) =>
          val arguments = macroArgs(typer, app)
          val runtime = macroRuntime(app)

          try {
            runtime(arguments) match {
              case expanded: scala.meta.internal.ast.Tree =>
                import scala.meta.dialects.Scala211
                import scala.meta.Scalahost

                val gTree = Scalahost.mkGlobalContext(global).toGtree(expanded).asInstanceOf[Tree]
                Some(typer.typed(gTree, pt))

              case _ =>
                None
            }
          } catch {
            case InvalidMacroInvocationException(msg) =>
              typer.context.error(expandee.pos, msg)
              None
          }

        case _ => None
      }
    }

    /**
     * Computes the arguments that need to be passed to the macro impl corresponding to a particular expandee.
     *
     * Default implementation provided in `self.standardMacroArgs` instantiates a `scala.reflect.macros.contexts.Context`,
     * gathers type and value arguments of the macro application and throws them together into `MacroArgs`.
     *
     * $nonCumulativeReturnValueDoc.
     */
    override def pluginsMacroArgs(typer: Typer, expandee: Tree): Option[MacroArgs] = {
      import scala.meta.dialects.Scala211
      import scala.meta.Input.String.{ apply => string2input }

      expandee match {
        case ParserMacroApplication(treeInfo.Applied(core, _, _), rawArguments, _) =>
          val prefix = core match { case Select(qual, _) => qual ; case _ => EmptyTree }
          val context = macroContext(typer, prefix, expandee)
          val tokenized = rawArguments map string2input map (_.tokens)
          Some(MacroArgs(context, tokenized))

        case _ => None
      }
    }

    /**
     * Summons a function that encapsulates macro implementation invocations for a particular expandee.
     *
     * Default implementation provided in `self.standardMacroRuntime` returns a function that
     * loads the macro implementation binding from the macro definition symbol,
     * then uses either Java or Scala reflection to acquire the method that corresponds to the impl,
     * and then reflectively calls into that method.
     *
     * $nonCumulativeReturnValueDoc.
     */
    override def pluginsMacroRuntime(expandee: Tree): Option[MacroRuntime] = {
      expandee match {
        case ParserMacroApplication(_, _, ParserMacroBinding(binding)) =>
          binding.fold(
            legacyBinding => legacyRuntime(legacyBinding),
            scalametaBinding => scalametaRuntime(scalametaBinding)
          )

        case _ =>
          None
      }
    }

    /**
     * Extractor for parser macro applications.
     */
    private object ParserMacroApplication {
      def unapply(tree: Tree): Option[(Tree, List[String], ParserMacroBinding)] = {

        val binding = tree.symbol.annotations.filter(_.atp.typeSymbol == MacroImplAnnotation) match {
          case AnnotationInfo(_, List(pickle), _) :: Nil                        => Some(new ParserMacroBinding(MacroImplBinding.unpickle(pickle)))
          case _ :: AnnotationInfo(_, List(ScalahostSignature(ddef)), _) :: Nil => Some(new ParserMacroBinding(ddef))
          case _                                                                => None
        }

        (tree.attachments.get[ParserMacroArgumentsAttachment].toList, binding) match {
          case (ParserMacroArgumentsAttachment(arguments) :: Nil, Some(binding)) =>
            Some((tree, arguments, binding))

          case _ =>
            None
        }
      }
    }

    private case class ParserMacroBinding(binding: Either[MacroImplBinding, DefDef]) {
      def this(binding: MacroImplBinding) = this(Left(binding))
      def this(ddef: DefDef) = this(Right(ddef))
    }

  }
}
