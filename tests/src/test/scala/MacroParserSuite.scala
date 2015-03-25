import org.scalatest.FunSuite
abstract class MacroParserSuite extends FunSuite {
  import compiler.Compiler._

  implicit class CompileAndCheck(code: String) {

    /**
     * Verifies that `code` actually expands to `expected`
     * during compilation.
     * The expansion is simply converted to a string that is
     * then compared to `expected`.
     */
    def shouldExpandTo(expected: String): Unit = {
      val expansion = compile(code).toString
      assert(expected == expansion,
        s"""\nExpected : $expected
           |Expansion: $expansion""".stripMargin)
    }

    /**
     * Verifies that a parser macro definition is rejected by the plugin.
     * Because we don't want to be too restrictive and issue an error for a macro
     * that may be any other flavour of macro, we check that the compiler issues
     * "macro definition needs to be enabled".
     */
    def shouldNotBeConsideredAParserMacro: Unit = {
      try {
        compile(code)
        fail("An error was expected during the compilation, but none was issued.")
      } catch {
        case err @ CompilationFailed(msg) =>
          assert(msg contains "macro definition needs to be enabled")
      }
    }

    /**
     * Verifies that an error is detected during the compilation
     * of `code`, and that the error message contains `expected`.
     */
    def shouldFailWith(expected: String): Unit = {
      try {
        compile(code)
        fail("An error was expected during the compilation, but none was issued.")
      } catch {
        case err @ CompilationFailed(msg) =>
          assert(msg contains expected)
      }
    }
  }


}
