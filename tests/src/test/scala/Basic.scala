import compiler.Compiler._
import org.scalatest.FunSuite

import scala.tools.reflect.{ ToolBoxError => CompilationFailure }

import scala.meta._
import scala.meta.dialects.Scala211

class BasicSuite extends MacroParserSuite {

  test("Simple expansion") {
    // We are actually giving 5 tokens to the parser macro: `BOF`, "hello", ` `, "world", `EOF`
    "macros.Macros.countTokens#(hello world)" shouldExpandTo "5"
  }

  test("Expand multi parameter parser macro") {
    // This macro always returns 1
    "macros.Macros.alwaysReturnOne#(hello)#(world)" shouldExpandTo "1"
  }

  test("Vanilla macros should still work") {
    // This macro always returns 1
    "macros.VanillaMacros.vanilla" shouldExpandTo "(1: Int)"
  }

  test("Expansion should match the expected type") {
    // This macro returns an Int, we are assigning it to a Boolean variable
    "val hello: Boolean = macros.Macros.countTokens#(hello world)" shouldFailWith "type mismatch"
  }

  test("Reject a macro whose owner is not an object") {
    """import scala.meta._
      |class Incorrect {
      |  def impl(t: Seq[Token]): Tree = scala.meta.internal.ast.Lit.Int(2)
      |  def hello: Int = macro impl
      |}""".stripMargin shouldFailWith "macro implementation reference has wrong shape"
  }

  test("Reject a macro whose implementation is private") {
    """import scala.meta._
      |object Incorrect {
      |  private def impl(t: Seq[Token]): Tree = scala.meta.internal.ast.Lit.Int(2)
      |  def hello: Int = macro impl
      |}""".stripMargin shouldFailWith "macro implementation must be public"
  }

  test("Reject a macro that has more than one parameter lists") {
    """object Incorrect {
      |  def hello: Int = macro macros.Macros.tooManyParamLists
      |}""".stripMargin shouldFailWith "macro parser can have only one parameter list"
  }

  test("Reject a macro whose parameters are incompatible with parser macros") {
    // We just want to make sure that we determine that the macro implementation cannot be used
    // as a parser macro, but don't output any error (after all, it may still be a valid whatever-you-want macro)
    """object Incorrect {
      |  def hello: Int = macro macros.Macros.incompatibleParameterTypes
      |}""".stripMargin.shouldNotBeConsideredAParserMacro
  }

  test("Reject a macro whose parameters are a subtype of `Seq[Token]`") {
    // We just want to make sure that we determine that the macro implementation cannot be used
    // as a parser macro, but don't output any error (after all, it may still be a valid whatever-you-want macro)
    """object Incorrect {
      |  def hello: Int = macro macros.Macros.incompatibleParameterTypes
      |}""".stripMargin.shouldNotBeConsideredAParserMacro
  }

  test("Accept a macro whose parameters are a supertype of `Seq[Token]`") {
    // This macro always return 1
    "macros.Macros.compatibleParameterType#(hello)" shouldExpandTo "1"
  }

  test("Accept a macro whose return type is a subtype of `Tree`") {
    // This macro always return 1
    "macros.Macros.compatibleReturnType#(hello)" shouldExpandTo "1"
  }

  test("Reject a macro whose return type is incompatible with parser macros") {
    """object Incorrect {
      |  def hello: Int = macro macros.Macros.incompatibleReturnType
      |}""".stripMargin shouldFailWith "must return a value of type scala.meta.Tree"
  }

  test("Fail on multi parameter macros when not enough arguments are given") {
    // This macro normally takes 2 arguments
    "macros.Macros.alwaysReturnOne#(hello)" shouldFailWith "parser macro expected 2 but got 1 argument"
  }
}
