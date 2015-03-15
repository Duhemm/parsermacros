import compiler.Compiler._
import org.scalatest.FunSuite

import scala.tools.reflect.{ ToolBoxError => CompilationFailure }

import scala.meta._
import scala.meta.dialects.Scala211

class BasicSuite extends FunSuite {
  // Currently we simply check that the string representation of the expansion
  // matches what we expect...

  test("Simple expansion") {
    val code = "macros.Macros.countTokens#(hello world)"
    val expansion = compile(code).toString
    val expected = "5" // BOF, "hello", ` `, "world", EOF
    assert(expected == expansion,
      s"""\nExpected : $expected
           |Expansion: $expansion""".stripMargin)
  }

  test("Expansion should match the expected type") {
    intercept[CompilationFailure] {
      val code = "val hello: Boolean = macros.Macros.count#(hello world)"
      // Will throw an exception because `macros.Macro.count#(...)` expands to `Int.Lit`
      compile(code)
    }
  }
}
