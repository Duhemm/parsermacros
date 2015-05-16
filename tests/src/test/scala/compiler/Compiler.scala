package compiler

import scala.reflect.internal.util.{ BatchSourceFile, NoFile }
import scala.reflect.internal.util.Position

import scala.tools.cmd.CommandLineParser
import scala.tools.nsc.{ CompilerCommand, Global, Settings }
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.AbstractReporter

/**
 * Helper object to compile code snippets to a virtual directory.
 */
object Compiler {

  case class CompilationFailed(msg: String) extends Exception(msg)

  /**
   * Compiles the given code and returns its AST.
   */
  def compileBlackbox(code: String): Global#Tree =
    compile(code, options = ParserMacro)

  /**
   * Compiles the given whitebox macro and returns its AST
   */
  def compileWhitebox(code: String): Global#Tree =
    compile(code, options = Paradise, ParserMacro)

  /**
   * Parses the given code and returns its AST.
   */
  def parse(code: String, wrapped: Boolean): Global#Tree = {
    val global = getCompiler(options = ParserMacro, Paradise, "-Ystop-after:parser")
    import global._
    val source = new BatchSourceFile(NoFile, if (wrapped) wrap(code) else code)
    val run = new Run
    run.compileSources(source :: Nil)
    val result = run.units.next.body
    if (wrapped) unWrap(global)(result) else result
  }

  /**
   * Compiles the given code, passing the given options to the compiler
   */
  private def compile(code: String, options: CompilerOption*): Global#Tree = {
    val global = getCompiler(options = options: _*)
    import global._
    val source = new BatchSourceFile(NoFile, wrap(code))
    val run = new Run
    run.compileSources(source :: Nil)
    unWrap(global)(run.units.next.body)
  }

  private def reportError(error: String) = throw CompilationFailed(error)

  /**
   * Reporter that ignores INFOs and WARNINGs, but directly aborts the compilation
   * on ERRORs.
   */
  private class TestReporter(override val settings: Settings) extends AbstractReporter {
    override def display(pos: Position, msg: String, severity: Severity): Unit = severity match {
      case INFO | WARNING => ()
      case ERROR          => reportError(msg)
    }

    override def displayPrompt(): Unit = ()
  }

  /**
   * Represents a basic compiler option (the string given to the command line invocation
   * of scalac)
   */
  private implicit class CompilerOption(s: String) {
    override def toString: String = s
  }

  /**
   * An option to add a compiler plugin
   */
  private class CompilerPlugin(jarPath: String, classpath: List[String])
    extends CompilerOption(s"-Xplugin:$jarPath" + (if (classpath.nonEmpty) classpath.mkString(" -cp ", ":", "") else ""))

  /**
   * Option to add the ParserMacro compiler plugin
   */
  private case object ParserMacro extends CompilerPlugin(
      jarPath = sys props "sbt.paths.plugin.jar",
      classpath = List(sys props "sbt.class.directory", sys props "sbt.paths.plugin.jar"))

  /**
   * Option to add Macro Paradise to the compiler plugins
   */
  private case object Paradise extends CompilerPlugin(
      jarPath = sys props "sbt.path.paradise.jar",
      classpath = Nil)

  /**
   * Returns an instance of `Global` configured according to the given options.
   */
  private def getCompiler(options: CompilerOption*): Global = {
    // I don't really know how I can reset the compiler after a run, nor what else
    // should also be reset, so for now this method creates new instances of everything,
    // which is not so cool.
    val arguments = CommandLineParser.tokenize(options mkString " ")
    val command = new CompilerCommand(arguments.toList, reportError _)
    val outputDir = new VirtualDirectory("(memory)", None)
    command.settings.outputDirs setSingleOutput outputDir
    val reporter = new TestReporter(command.settings)

    new Global(command.settings, reporter)
  }

  /**
   * Wraps a code snippet in an empty class (scalac refuses to compile isolated code snippets.)
   * Doesn't modify the code if it doesn't need to be wrapped
   */
  private def wrap(code: String): String = {
    if (code.startsWith("package") || code.startsWith("class") || code.startsWith("object")) code
    else s"class Compilation { $code }"
  }

  /**
   * Extracts very naively the result of a macro expansion from the output of scalac,
   * or returns its input if the extraction failed (it will fail if the tree didn't need to be wrapped).
   */
  private def unWrap(global: Global)(tree: global.Tree): global.Tree = {
    import global._

    tree match {
      case PackageDef(_, List(ClassDef(_, _, _, Template(_, _, List(DefDef(_, _, _, _, _, block: Block)))))) if block.stats.length == 2 =>
        block.stats(1)

      case _ =>
        tree
    }
  }

}
