import boxity.ParserMacroSuite

import scala.tools.nsc.Global

/**
 * These tests verify that the modified parser behaves as expected.
 */
class ScalaParserSuite extends ParserMacroSuite {

  override def compile(code: String) = throw new UnsupportedOperationException

  test("Simple class definition should be parsed correctly") {
    "class A extends B { def foo = bar }".shouldParse
  }

  test("Normal expressions should be parsed correctly inside object definition") {
    "object A { 2 + 2 }".shouldParse
  }

  test("Wrapping of expressions should allow them to be parsed") {
    "2 + 2".shouldParseWrapped
  }

  test("Normal expressions should be rejected outside object definition") {
    "2 + 2" shouldNotParseWith "expected class or object definition"
  }

  test("Parser macro application should be accepted in top level position") {
    "foo.bar#(hello world)".shouldParse
  }

  test("Multi-parameters parser macro applications should be accepted in top level position") {
    "foo.bar#(hello)#(world)".shouldParse
  }

  test("Parsing two parser macro application following each other") {
    """foo.bar#(hello world)
      |bar.foo#(hola mundo)""".stripMargin.shouldParse
  }

  test("Parser macro application then class definition") {
    """foo.bar#(hello world)
      |class A extends B""".stripMargin.shouldParse
  }

  test("Class definition then parser macro application") {
    """class A { def foo = bar + 3 }
      |foo.bar.shazam#(booooh)""".stripMargin.shouldParse
  }

  test("Parser macro sandwiched between two class declarations") {
    """class A { def a = b }
      |foo.bar.shazam#(hello)
      |class B { def b = a }""".stripMargin.shouldParse
  }

  test("Class declaration sandwiched between two parser macro applications") {
    """foo.bar#(yipee)
      |object O
      |bar.foo.boum#(blah)""".stripMargin.shouldParse
  }

  test("Normal function application should be rejection in top level position") {
    "foo.bar(2)" shouldNotParseWith "only parser macros can be called at top-level."
  }

  test("Parser macro application with name formed of only one part should be accepted") {
    "Foo#(tadaaaa)".shouldParse
  }

  test("Arbitrary expressions involving parser macro in top level positions should not be accepted") {
    "foo.bar#(hello) == 123" shouldNotParseWith "';' expected but identifier found."
  }

  test("Arbitrary expressions involving parser macro in an object definition should be accepted") {
    "foo.bar#(hello) == 123".shouldParseWrapped
  }

  test("Parser macro application should be accepted inside object definition") {
    "foo.bar#(hello world)".shouldParseWrapped
  }

  test("Parser macro application in the body of a function should be rewritten") {
    """class A {
      |  def foo(x: Int): Int = {
      |    magnificent.parsermacro#(application)
      |    123
      |  }
      |}""".stripMargin parseAndCheck { case classDef: Global#ClassDef =>

        val fooDef             = classDef.impl.body(1).asInstanceOf[Global#DefDef]
        val fooBody            = fooDef.rhs.asInstanceOf[Global#Block]
        val syntheticModuleDef = fooBody.stats(0).asInstanceOf[Global#ModuleDef]

        checkSyntheticModuleDef(syntheticModuleDef)
    }
  }

  test("Parser macro application in class constructor should be rewritten") {
    """class A {
      |  foo.bar#(hello world)
      |}""".stripMargin parseAndCheck { case classDef: Global#ClassDef =>

        val macroParserApplication = classDef.impl.body(1)
        assert(macroParserApplication.isDef)

        val syntheticModuleDef = macroParserApplication.asInstanceOf[Global#ModuleDef]
        checkSyntheticModuleDef(syntheticModuleDef)
    }
  }

  private def checkSyntheticModuleDef(moduleDef: Global#ModuleDef): Unit = {
    assert(moduleDef.name.toString == "TemporaryObject")
    assert(moduleDef.mods.annotations.nonEmpty, s"Synthetic module should have at least one annotation but none was found.")

    val syntheticValDef = moduleDef.impl.body(1).asInstanceOf[Global#ValDef]
    checkSyntheticValDef(syntheticValDef)
  }

  private def checkSyntheticValDef(valDef: Global#ValDef): Unit = {
    assert(valDef.name.toString == "tokens", s"Rewritten val should be named `tokens` but was `${valDef.name}`")
  }


}
