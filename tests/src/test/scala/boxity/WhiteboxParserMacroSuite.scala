package boxity

/**
 * Trait for whitebox parser macro tests
 */
trait WhiteboxParserMacroSuite extends ParserMacroSuite {
  override def compile(code: String) = compiler.Compiler.compileWhitebox(code)
}
