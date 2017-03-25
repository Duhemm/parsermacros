package suites

import boxity.ParserMacroSuite

trait CorrectParserMacros extends ParserMacroSuite {

  test("Simple expansion") {
    // We are actually giving 5 tokens to the parser macro: `BOF`, "hello", ` `, "world", `EOF`
    "macros.ParserMacros.countTokens#(hello world)" shouldExpandTo "5"
  }

  test("Expand multi parameter parser macro") {
    // This macro always returns 1
    "macros.ParserMacros.multiParameter#(hello)(world)" shouldExpandTo "1"
  }

  test("Vanilla macros should still work") {
    // This macro always returns 1
    "macros.VanillaMacros.vanilla" shouldExpandTo "(1: Int)"
  }

  test("Accept a macro whose parameters are a supertype of `Seq[Token]`") {
    // This macro always return 1
    "macros.ParserMacros.compatibleParameterType#(hello)" shouldExpandTo "1"
  }

  test("Accept a macro whose return type is a subtype of `Tree`") {
    // This macro always return 1
    "macros.ParserMacros.compatibleReturnType#(hello)" shouldExpandTo "1"
  }

  test("Accept macros that have type parameters") {
    // This macro always return 1
    "macros.ParserMacros.hasTypeParameters[Int]#(hello)" shouldExpandTo "1"
  }

  test("Accept macros that use their type type parameters") {
    // This macro always return 1
    "macros.ParserMacros.hasTypeParametersToo[Any]#(hello)" shouldExpandTo "1"
  }

  test("Accept macros defined inside a package object") {
    """import scala.meta._
      |package object incorrect {
      |  def hello(t: Tokens): Tree = macro { scala.meta.Lit(1) }
      |}""".stripMargin.shouldCompile
  }

  test("Accept macros defined in a deeply nested package") {
    // This macro always return 1
    "macros.deeply.nested.pkg.DeeplyNestedParserMacro.Deeply.Nested.Obj.foo#(hello)" shouldExpandTo "1"
  }

  test("Expand parser macros defined in the empty package") {
    "InEmptyPackage.foo#(hello)" shouldExpandTo "1"
  }
}
