package boxity

/**
 * Trait for blackbox parser macro tests
 */
trait BlackboxParserMacroSuite extends ParserMacroSuite {
  override def compile(code: String) = compiler.Compiler.compileBlackbox(code)
}
