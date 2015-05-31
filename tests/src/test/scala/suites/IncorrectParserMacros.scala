package suites

import boxity.ParserMacroSuite

trait IncorrectParserMacros extends ParserMacroSuite {

  test("Expansion should match the expected type") {
    // This macro returns an Int, we are assigning it to a Boolean variable
    "val hello: Boolean = macros.ParserMacros.countTokens#(hello world)" shouldFailWith "type mismatch"
  }

  test("Reject a macro whose owner is not an object") {
    """import scala.meta._
      |class Incorrect {
      |  def hello(tokens: Seq[Token]) = macro { internal.ast.Lit.Int(1) }
      |}""".stripMargin shouldFailWith "macro implementation reference has wrong shape"
  }

  test("Reject a macro that has more than one parameter lists") {
    """import scala.meta._
      |object Incorrect {
      |  def hello(t1: Seq[Token])(t2: Seq[Token]) = macro { internal.ast.Lit.Int(1) }
      |}""".stripMargin shouldFailWith "parser macro can have only one parameter list"
  }

  test("Reject a macro whose parameters are incompatible with parser macros") {
    // We just want to make sure that we determine that the macro implementation cannot be used
    // as a parser macro, but don't output any error (after all, it may still be a valid whatever-you-want macro)
    """import scala.meta._
      |object Incorrect {
      |  def hello(x: Map[Int, String]) = macro { internal.ast.Lit.Int(1) }
      |}""".stripMargin.shouldNotBeConsideredAParserMacro
  }

  test("Reject a macro whose parameters are a subtype of `Seq[Token]`") {
    // We just want to make sure that we determine that the macro implementation cannot be used
    // as a parser macro, but don't output any error (after all, it may still be a valid whatever-you-want macro)
    """import scala.meta._
      |object Incorrect {
      |  def hello(t: List[Token]) = macro { internal.ast.Lit.Int(1) }
      |}""".stripMargin.shouldNotBeConsideredAParserMacro
  }

  test("Reject a macro whose return type is incompatible with parser macros") {
    """import scala.meta._
      |object Incorrect {
      |  def hello(t: Seq[Token]) = macro { 1 }
      |}""".stripMargin shouldFailWith "must return a value of type scala.meta.Tree"
  }

  test("Fail on multi parameter macros when not enough arguments are given") {
    // This macro normally takes 2 arguments
    "macros.ParserMacros.multiParameter#(hello)" shouldFailWith "parser macro expected 2 but got 1 argument"
  }

  test("Fail on multi parameter macros when too many arguments are given") {
    // This macro normally takes 2 arguments
    "macros.ParserMacros.multiParameter#(hello)#(world)#(!)" shouldFailWith "parser macro expected 2 but got 3 arguments"
  }

  test("Reject parser macro with implicit parameters") {
    """import scala.meta._
      |object Incorrect {
      |  def hello(implicit tokens: Seq[Token]): Tree = macro { internal.ast.Lit.Int(1) }
      |}""".stripMargin shouldFailWith "parser macro cannot have implicit parameters"
  }

}
