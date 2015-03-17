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

  implicit class ShouldExpandTo(code: String) {

    def shouldExpandTo(expected: String): Unit = {
      val expansion = compile(code).toString
      assert(expected == expansion,
        s"""\nExpected : $expected
           |Expansion: $expansion""".stripMargin)
    }

    def shouldNotBeConsideredAParserMacro: Unit = {
      val expansion = compile(code)
      assert(hasError(expansion))
    }

    def shouldFailWith(expected: String): Unit = {
      try {
        compile(code)
        fail()
      } catch {
        case err @ ToolBoxError(msg, _) =>
          assert(msg contains expected)
      }
    }
  }


}
