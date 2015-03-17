import org.scalatest.FunSuite
import scala.tools.reflect.ToolBoxError

abstract class MacroParserSuite extends FunSuite {
  import compiler.Compiler._

  /**
   * Determines whether or not the product of the toolbox contains error
   * (typically a tree that contains an error because a macro could not expand)
   */
  def hasError(tree: tb.u.Tree): Boolean =
    tree.toString.contains(": error>") // TODO: Improve this

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
     * Verifies that `code` contains an error after compilation,
     * according to `hasError`.
     */
    def shouldNotBeConsideredAParserMacro: Unit = {
      val expansion = compile(code)
      assert(hasError(expansion))
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
        case err @ ToolBoxError(msg, _) =>
          assert(msg contains expected)
      }
    }
  }


}
