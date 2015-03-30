/**
 * These tests verify that all the constraints on macro implementations are
 * respected, and that the plugin behaves correctly in unexpected situations
 * (for instance when a parser macro is not given enough arguments).
 */
class IncorrectLegacyMacrosSuite extends MacroParserSuite {

  test("Expansion should match the expected type") {
    // This macro returns an Int, we are assigning it to a Boolean variable
    "val hello: Boolean = macros.LegacyMacros.countTokens#(hello world)" shouldFailWith "type mismatch"
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
      |  def hello: Int = macro macros.LegacyMacros.tooManyParamLists
      |}""".stripMargin shouldFailWith "parser macro can have only one parameter list"
  }

  test("Reject a macro whose parameters are incompatible with parser macros") {
    // We just want to make sure that we determine that the macro implementation cannot be used
    // as a parser macro, but don't output any error (after all, it may still be a valid whatever-you-want macro)
    """object Incorrect {
      |  def hello: Int = macro macros.LegacyMacros.incompatibleParameterTypes
      |}""".stripMargin.shouldNotBeConsideredAParserMacro
  }

  test("Reject a macro whose parameters are a subtype of `Seq[Token]`") {
    // We just want to make sure that we determine that the macro implementation cannot be used
    // as a parser macro, but don't output any error (after all, it may still be a valid whatever-you-want macro)
    """object Incorrect {
      |  def hello: Int = macro macros.LegacyMacros.incompatibleParameterSeqType
      |}""".stripMargin.shouldNotBeConsideredAParserMacro
  }

  test("Reject a macro whose return type is incompatible with parser macros") {
    """object Incorrect {
      |  def hello: Int = macro macros.LegacyMacros.incompatibleReturnType
      |}""".stripMargin shouldFailWith "must return a value of type scala.meta.Tree"
  }

  test("Fail on multi parameter macros when not enough arguments are given") {
    // This macro normally takes 2 arguments
    "macros.LegacyMacros.alwaysReturnOne#(hello)" shouldFailWith "parser macro expected 2 but got 1 argument"
  }

  test("Fail on multi parameter macros when too many arguments are given") {
    // This macro normally takes 2 arguments
    "macros.LegacyMacros.alwaysReturnOne#(hello)#(world)#(!)" shouldFailWith "parser macro expected 2 but got 3 arguments"
  }

  test("Reject parser macro with implicit parameters") {
    """import scala.meta._
      |object Incorrect {
      |  def impl(implicit tokens: Seq[Token]): Tree = ???
      |  def foo: Int = macro impl
      |}""".stripMargin shouldFailWith "parser macro cannot have implicit parameters"
  }
}
