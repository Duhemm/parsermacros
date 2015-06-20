package org.duhemm.parsermacro

import scala.tools.nsc.ast.parser.{ SyntaxAnalyzer => NscSyntaxAnalyzer, TreeBuilder }
import scala.reflect.internal.Phase

import scala.tools.nsc.ast.parser.BracePatch
import scala.tools.nsc.ast.parser.Tokens._

import scala.reflect.internal.Flags
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Stack

// Partially taken from scalameta/scalahost
/**
 * New syntax analyzer that behaves just like scalac's default syntax analyzer, but is able
 * to parse parser macro applications.
 */
abstract class ParserMacroSyntaxAnalyzer extends NscSyntaxAnalyzer with ParserMacroSyntheticObject {
  import global._

  val universe = global.rootMirror.universe

  override val runsAfter: List[String] = Nil
  override val runsRightAfter: Option[String] = None
  override val initial = true

  private def initialUnitBody(unit: CompilationUnit): Tree = {
    if (unit.isJava) new JavaUnitParser(unit).parse()
    else if (currentRun.parsing.incompleteHandled) new ParserMacroUnitParser(unit).parse()
    else new ParserMacroUnitParser(unit).smartParse()
  }

  def newUnitParser(unit: CompilationUnit): UnitParser = new ParserMacroUnitParser(unit)
  private class ParserMacroUnitParser(unit: global.CompilationUnit, patches: List[BracePatch]) extends UnitParser(unit, patches) {
    def this(unit: global.CompilationUnit) = this(unit, Nil)
    override def withPatches(patches: List[BracePatch]): UnitParser = new UnitParser(unit, patches)
    override def newScanner() = new UnitScanner(unit, patches)

    override def blockStatSeq(): List[Tree] =
      super.blockStatSeq() map transformParserMacroApplication

    override def templateStatSeq(isPre : Boolean) = {
      val (self, stats) = super.templateStatSeq(isPre)
      (self, stats map transformParserMacroApplication)
    }

    override def templateStat: PartialFunction[Token, List[Tree]] = {
      case t: Token if super.templateStat isDefinedAt t =>
        super.templateStat(t) map transformParserMacroApplication
    }

    override def topStat: PartialFunction[Token, List[Tree]] = {
      case _ if isIdent =>
        transformParserMacroApplication(parserMacro) :: Nil
      case t if super.topStat isDefinedAt t =>
        super.topStat(t)
      case _ =>
        syntaxErrorOrIncompleteAnd("expected class or object definition", skipIt = true)(Nil)
    }

    private def parserMacro: Tree = {
      val base = qualId()
      if (in.token == DOT) parserMacroApplication(selectors(base, false, in.offset))
      else parserMacroApplication(base)
    }

    private def parserMacroApplication(t: Tree): Tree = {
      in.token match {
        case HASH =>
          def isOpeningParenOrBrace(t: Token) = t == LPAREN || t == LBRACE
          def closingFor(t: Token) = t match {
            case LPAREN => RPAREN
            case LBRACE => RBRACE
            case _      => ???
          }
          in.nextToken()
          val macroParserArgs = new ListBuffer[String]

          while (isOpeningParenOrBrace(in.token)) {
            val closingStack = Stack(closingFor(in.token))
            val start = in.offset + 1

            while (!closingStack.isEmpty && in.token != ERROR && in.token != EOF) {
              in.nextToken()
              if (isOpeningParenOrBrace(in.token)) {
                closingStack push closingFor(in.token)
              } else if (in.token == closingStack.head) {
                closingStack.pop()
              }
            }

            if (!closingStack.isEmpty) {
              syntaxErrorOrIncompleteAnd(s"expected ${token2string(closingStack.head)}, but ${token2string(in.token)} found.", skipIt = true)(EmptyTree)
            }

            val end = in.offset
            in.nextToken()
            macroParserArgs += in.buf.slice(start, end) mkString ""
          }
          t updateAttachment ParserMacroArgumentsAttachment(macroParserArgs.toList)

        case other =>
          syntaxErrorOrIncompleteAnd(s"only parser macros can be called at top-level.", skipIt = true)(EmptyTree)

      }
    }

    override def simpleExprRest(t: Tree, canApply: Boolean): Tree = {
      if (canApply) newLineOptWhenFollowedBy(LBRACE)
      in.token match {
        case HASH if (canApply) =>
          parserMacroApplication(t)
        case _ =>
          super.simpleExprRest(t, canApply)
      }
    }
  }

  override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    override val checkable = false
    override val keepsTypeParams = false

    def apply(unit: CompilationUnit) {

      informProgress("parsing " + unit)
      // if the body is already filled in, don't overwrite it
      // otherwise compileLate is going to overwrite bodies of synthetic source files
      if (unit.body == EmptyTree)
        unit.body = initialUnitBody(unit)

      if (settings.Yrangepos && !reporter.hasErrors)
        validatePositions(unit.body)

      if (settings.Ymemberpos.isSetByUser)
        new MemberPosReporter(unit) show (style = settings.Ymemberpos.value)
    }
  }

  // We want to enable the rewritting of parser macro application to macro annotated declarations only if
  // Paradise is enabled.
  private lazy val enableWhitebox = plugins exists (_.getClass.getName contains "org.scalamacros.paradise.Plugin")

  /**
   * Rewrites a parser macro application to a macro annotated module def. The argument of
   * the macro annotation is the selection of the parser macro (something like Foo.Bar.baz).
   * The module def contains a single value definition which holds the parameters of the parser macro.
   * The object will be replaced by the expansion of the parser macro.
   */
  private def transformParserMacroApplication(tree: Tree): Tree = {
    if (enableWhitebox && tree.hasAttachment[ParserMacroArgumentsAttachment]) {
      val tokens = tree.attachments.get[ParserMacroArgumentsAttachment].toList.head.args
      TemporaryObject(tree, tokens)
    } else {
      tree
    }
  }


}
