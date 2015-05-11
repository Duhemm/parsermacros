import boxity.WhiteboxParserMacroSuite
import syntaxes.legacy.LegacyCorrectMacros

class WhiteboxLegacyCorrectMacros extends WhiteboxParserMacroSuite with LegacyCorrectMacros {

  test("Expanding to a new function inside a function should work") {
    """def foo(x: Int) {
      |  macros.LegacyWhitebox.addMethodNamed#(bar)
      |  bar
      |}""".stripMargin.shouldCompile
  }

}
