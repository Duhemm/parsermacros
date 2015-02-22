package org.duhemm.parsermacro

import scala.reflect.internal.Mode

trait AnalyzerPlugins { self: Plugin =>
  import global._
  import analyzer._

  object MacroPlugin extends analyzer.MacroPlugin {
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
      val DefDef(mods, name, tparams, vparamss, tpt, rhs) = ddef

      if(name.toString == "bar") Some(tpt)
      else None
    }

    /**
     * Figures out whether the given macro definition is blackbox or whitebox.
     *
     * Default implementation provided in `self.standardIsBlackbox` loads the macro impl binding
     * and fetches boxity from the "isBlackbox" field of the macro signature.
     *
     * $nonCumulativeReturnValueDoc.
     */
    override def pluginsIsBlackbox(macroDef: Symbol): Option[Boolean] = None

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

        if(expandee.toString == "Macros.bar") {
          val args = macroArgs(typer, expandee)
          val runtime = macroRuntime(expandee)
          val result = runtime(args)

          println("-" * 211)
          println(s"$expandee expanded to:")
          result match {
            case tree: scala.meta.Tree =>
              import scala.meta.dialects.Scala211
              import scala.meta.ui._
              println("Code: " + tree.show[Code])
              println("Raw : " + tree.show[Raw])

            case other =>
              println(other)
              throw new Exception(s"Macro expansion should be an instance of scala.meta.Tree, found ${other.getClass.getName}.")
          }
          println("-" * 211)
          Some(typer.typed(Literal(Constant(12))))
        }
        else None
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
      import scala.meta.syntactic.tokenizers
      import scala.meta.Origin
      val args = expandee.attachments.get[List[String]]

      if(args.isEmpty) {
        None
      } else {
        val args = expandee.attachments.get[List[String]].head
        val origins = args.map(a => Origin.String(a))
        val a2 = origins.map(origin => tokenizers.tokenize(origin))
        val a3 = MacroArgs(context(typer, null, expandee), a2)
        Some(a3)
      }
    }

    private def context(typer: Typer, prefixTree: Tree, expandeeTree: Tree) = {
      new {
        val universe: self.global.type = self.global
        val callsiteTyper: universe.analyzer.Typer = typer.asInstanceOf[global.analyzer.Typer]
        val expandee = universe.analyzer.macroExpanderAttachment(expandeeTree).original orElse duplicateAndKeepPositions(expandeeTree)
      } with UnaffiliatedMacroContext {
        val prefix = Expr[Nothing](prefixTree)(TypeTag.Nothing)
        override def toString = "MacroContext(%s@%s +%d)".format(expandee.symbol.name, expandee.pos, enclosingMacros.length - 1 /* exclude myself */)
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
      val args = expandee.attachments.get[List[String]]
      if(args.isEmpty) {
        None
      } else {
        import scala.meta.syntactic.tokenizers.Token
        val method = expandee.symbol
        val owner = method.owner

        val classpath = global.classPath.asURLs
        val classloader = scala.reflect.internal.util.ScalaClassLoader.fromURLs(classpath, self.getClass.getClassLoader)

        val implClass = Class.forName(owner.fullName, true, classloader)
        val implMethod = implClass.getMethods.find(_.getName == "foo").get

        val fun = (x: MacroArgs) => implMethod.invoke(implClass, x.others)
        Some(fun)
      }
    }

    /**
     * Creates a symbol for the given tree in lexical context encapsulated by the given namer.
     *
     * Default implementation provided in `namer.standardEnterSym` handles MemberDef's and Imports,
     * doing nothing for other trees (DocDef's are seen through and rewrapped). Typical implementation
     * of `enterSym` for a particular tree flavor creates a corresponding symbol, assigns it to the tree,
     * enters the symbol into scope and then might even perform some code generation.
     *
     * $nonCumulativeReturnValueDoc.
     */
    override def pluginsEnterSym(namer: Namer, tree: Tree): Boolean = false

    /**
     * Makes sure that for the given class definition, there exists a companion object definition.
     *
     * Default implementation provided in `namer.standardEnsureCompanionObject` looks up a companion symbol for the class definition
     * and then checks whether the resulting symbol exists or not. If it exists, then nothing else is done.
     * If not, a synthetic object definition is created using the provided factory, which is then entered into namer's scope.
     *
     * $nonCumulativeReturnValueDoc.
     */
    override def pluginsEnsureCompanionObject(namer: Namer, cdef: ClassDef, creator: ClassDef => Tree = companionModuleDef(_)): Option[Symbol] = None

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
    override def pluginsEnterStats(typer: Typer, stats: List[Tree]): List[Tree] = stats

  }
}