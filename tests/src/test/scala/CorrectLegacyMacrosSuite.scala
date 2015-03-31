/**
 * These tests verify that correct parser macros work as expected, and that everything that
 * should be considered a valid parser macro is actually accepted.
 * They also test that the plugin doesn't interfere with the vanilla macros.
 */
class CorrectLegacyMacrosSuite extends MacroParserSuite {

  test("Simple expansion") {
    // We are actually giving 5 tokens to the parser macro: `BOF`, "hello", ` `, "world", `EOF`
    "macros.LegacyMacros.countTokens#(hello world)" shouldExpandTo "5"
  }

  test("Expand multi parameter parser macro") {
    // This macro always returns 1
    "macros.LegacyMacros.alwaysReturnOne#(hello)#(world)" shouldExpandTo "1"
  }

  test("Vanilla macros should still work") {
    // This macro always returns 1
    "macros.VanillaMacros.vanilla" shouldExpandTo "(1: Int)"
  }

  test("Accept a macro whose parameters are a supertype of `Seq[Token]`") {
    // This macro always return 1
    "macros.LegacyMacros.compatibleParameterType#(hello)" shouldExpandTo "1"
  }

  test("Accept a macro whose return type is a subtype of `Tree`") {
    // This macro always return 1
    "macros.LegacyMacros.compatibleReturnType#(hello)" shouldExpandTo "1"
  }

  test("Accept a macro whose implementation is defined in an abstract parent") {
    // This macro always return 1
    "macros.ConcreteProvider.concreteImplInAbstractParent#(hello)" shouldExpandTo "1"
  }

  test("Accept a macro whose implementation is overridden from an abstract parent") {
    // This macro always return 1
    "macros.ConcreteProvider.overrideAbstractImplFromParent#(hello)" shouldExpandTo "1"
  }

  test("Accept macros that have type parameters") {
    // This macro always return 1
    "macros.LegacyMacros.hasTypeParameters[Int]#(hello)" shouldExpandTo "1"
  }

  test("Accept macros that use their type type parameters") {
    // This macro always return 1
    "macros.LegacyMacros.hasTypeParametersToo[Any]#(hello)" shouldExpandTo "1"
  }

  test("Accept macros defined inside a package object") {
    pendingUntilFixed {
      """import scala.meta._
        |package object incorrect {
        |  def impl(t: Seq[Token]): Tree = internal.ast.Lit.Int(1)
        |  def hello: Int = macro impl
        |}""".stripMargin.shouldCompile
    }
  }

}
