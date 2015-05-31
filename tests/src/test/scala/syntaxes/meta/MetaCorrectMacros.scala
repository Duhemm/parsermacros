package syntaxes.meta

import boxity.ParserMacroSuite

trait MetaCorrectMacros extends ParserMacroSuite {

  test("Simple expansion") {
    // We are actually giving 5 tokens to the parser macro: `BOF`, "hello", ` `, "world", `EOF`
    "macros.LightweightMacros.countTokens#(hello world)" shouldExpandTo "5"
  }

  test("Expand multi parameter parser macro") {
    // This macro always returns 1
    "macros.LightweightMacros.multiParameter#(hello)#(world)" shouldExpandTo "1"
  }

  test("Vanilla macros should still work") {
    // This macro always returns 1
    "macros.VanillaMacros.vanilla" shouldExpandTo "(1: Int)"
  }

  test("Accept a macro whose parameters are a supertype of `Seq[Token]`") {
    // This macro always return 1
    "macros.LightweightMacros.compatibleParameterType#(hello)" shouldExpandTo "1"
  }

  test("Accept a macro whose return type is a subtype of `Tree`") {
    // This macro always return 1
    "macros.LightweightMacros.compatibleReturnType#(hello)" shouldExpandTo "1"
  }

  test("Accept macros that have type parameters") {
    // This macro always return 1
    "macros.LightweightMacros.hasTypeParameters[Int]#(hello)" shouldExpandTo "1"
  }

  test("Accept macros that use their type type parameters") {
    // This macro always return 1
    "macros.LightweightMacros.hasTypeParametersToo[Any]#(hello)" shouldExpandTo "1"
  }

  test("Accept macros defined inside a package object") {
    pendingUntilFixed {
      """import scala.meta._
        |package object incorrect {
        |  def hello(t: Seq[Token]): Tree = macro { internal.ast.Lit.Int(1) }
        |}""".stripMargin.shouldCompile
    }
  }

  test("Accept macros defined in a deeply nested package") {
    // This macro always return 1
    "macros.deeply.nested.pkg.DeeplyNestedLightweight.Deeply.Nested.Obj.foo#(hello)" shouldExpandTo "1"
  }
}
