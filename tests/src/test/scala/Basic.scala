import compiler.Compiler._
import org.scalatest.FunSuite

import scala.tools.reflect.{ ToolBoxError => CompilationFailure }

import scala.meta._
import scala.meta.dialects.Scala211

class BasicSuite extends FunSuite {

  def hasError(tree: tb.u.Tree): Boolean =
    tree.toString.contains(": error>") // TODO: Improve this

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

  test("Expand multi parameter parser macro") {
    val code = "macros.Macros.alwaysReturnOne#(hello)#(world)"
    val expansion = compile(code).toString
    val expected = "1" // This macro always return 1
    assert(expected == expansion,
      s"""\nExpected : $expected
           |Expansion: $expansion""".stripMargin)
  }

  test("Vanilla macros should still work") {
    val code = "macros.VanillaMacros.vanilla"
    val expansion = compile(code).toString
    val expected = "(1: Int)"
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

  test("Reject a macro whose owner is not an object") {
    intercept[CompilationFailure] {
      val code = """import scala.meta._
                   |class Incorrect {
                   |  def impl(t: Seq[Token]): Tree = internal.ast.Lit.Int(2)
                   |  def hello: Int = macro impl
                   |}""".stripMargin
      compile(code)

    }
  }

  test("Reject a macro whose implementation is private") {
    intercept[CompilationFailure] {
      val code = """import scala.meta._
                   |object Incorrect {
                   |  private def impl(t: Seq[Token]): Tree = internal.ast.Lit.Int(2)
                   |  def hello: Int = macro impl
                   |}""".stripMargin
      compile(code)

    }
  }

  test("Reject a macro that has more than one parameter lists") {
    intercept[CompilationFailure] {
      val code = """object Incorrect {
                   |  def hello: Int = macro macros.Macros.tooManyParamLists
                   |}""".stripMargin
      compile(code)
    }
  }

  test("Reject a macro whose parameters are incompatible with parser macros") {
    val code = """object Incorrect {
                 |  def hello: Int = macro macros.Macros.incompatibleParameterTypes
                 |}""".stripMargin
    val expansion = compile(code)

    assert(hasError(expansion))
  }

  test("Reject a macro whose parameters are a subtype of `Seq[Token]`") {
    val code = """object Incorrect {
                 |  def hello: Int = macro macros.Macros.incompatibleParameterTypes
                 |}""".stripMargin
    val expansion = compile(code)

    assert(hasError(expansion))
  }

  test("Accept a macro whose parameters are a supertype of `Seq[Token]`") {
    val code = "macros.Macros.compatibleParameterType#(hello)"
    val expected = "1"
    val expansion = compile(code).toString
    assert(expected == expansion,
      s"""\nExpected : $expected
           |Expansion: $expansion""".stripMargin)
  }

  test("Accept a macro whose return type is a subtype of `Tree`") {
    val code = "macros.Macros.compatibleReturnType#(hello)"
    val expected = "1"
    val expansion = compile(code).toString
    assert(expected == expansion,
      s"""\nExpected : $expected
           |Expansion: $expansion""".stripMargin)
  }

  test("Reject a macro whose return type is incompatible with parser macros") {
    intercept[CompilationFailure] {
      val code = """object Incorrect {
                   |  def hello: Int = macro macros.Macros.incompatibleReturnType
                   |}""".stripMargin
      compile(code)
    }
  }

  test("Fail on multi parameter macros when not enough arguments are given") {
    intercept[CompilationFailure] {
      val code = "macros.Macros.alwaysReturnOne#(hello)" // This macro normally takes 2 arguments
      compile(code)
    }
  }
}
