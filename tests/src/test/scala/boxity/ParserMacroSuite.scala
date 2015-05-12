package boxity

import org.scalatest.FunSuite
import scala.tools.nsc.Global

/**
 * Common functionalities for black and whitebox parser macro tests
 */
trait ParserMacroSuite extends FunSuite {
  import compiler.Compiler.{ CompilationFailed, parse }

  def compile(code: String): Global#Tree

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

    /**
     * Verifies that the code compiles without errors
     */
    def shouldCompile: Unit = {
      try {
        compile(code)
      } catch {
        case err @ CompilationFailed(msg) =>
        fail(s"Compilation failed but success was expected:\n$msg")
      }
    }

    /**
     * Verifies that the given code can be sucessfully parsed without wrapping.
     */
    def shouldParse: Unit =
      shouldParse(wrapped = false)

    /**
     * Verifies that the given code can be successfully parsed after being
     * wrapped in a class definition.
     */
    def shouldParseWrapped: Unit =
      shouldParse(wrapped = true)

    /**
     * Verifies that parsing the given code issues a parsing error matching `expected`.
     * The code is not wrapped in a class definition before parsing.
     */
    def shouldNotParseWith(expected: String): Unit =
      shouldNotParseWith(expected, wrapped = false)

    /**
     * Verifies that parsing the given code issues a parsing error matching `expected`.
     * The code is wrapped in a class definition before parsing.
     */
    def shouldNotParseWrappedWith(expected: String): Unit =
      shouldNotParseWith(expected, wrapped = true)

    /**
     * Parses the code and verifies that the resulting tree satisfies `test`.
     * The code is not wrapped before it is parsed.
     */
    def parseAndCheck(test: Global#Tree => Unit): Unit = {
      try {
        val tree = extractFromEmptyPackage(parse(code, wrapped = false))
        test(tree)
      } catch {
        case CompilationFailed(msg) =>
          fail(s"Parsing failed but success was expected:\n$msg")
      }
    }

    /**
     * Extracts a single definition from the empty package. If there are more than one definitions,
     * then they will be lost.
     */
    private def extractFromEmptyPackage(tree: Global#Tree): Global#Tree = tree match {
      case pkg: Global#PackageDef if pkg.name.toString == "<empty>" =>
        pkg.stats.head

      case other =>
        other
    }

    private def shouldParse(wrapped: Boolean): Unit = {
      try {
        parse(code, wrapped)
      } catch {
        case CompilationFailed(msg) =>
          fail(s"Parsing failed but success was expected:\n$msg")
      }
    }

    private def shouldNotParseWith(expected: String, wrapped: Boolean): Unit = {
      try {
        parse(code, wrapped)
        fail("An error was expected during the parsing, but none was issued.")
      } catch {
        case CompilationFailed(msg) =>
          assert(msg contains expected)
      }
    }
  }
}
