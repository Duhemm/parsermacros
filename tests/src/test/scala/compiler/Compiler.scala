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

  private val compilerOptions: String = {
    val usePlugin = "-Xplugin:" + System.getProperty("sbt.paths.plugin.jar")
    val classpath = "-cp " + sys.props("sbt.class.directory") + ":" + sys.props("sbt.paths.plugin.jar")
    s"$usePlugin $classpath"
  }

  private def getCompiler: Global = {
    // I don't really know how I can reset the compiler after a run, nor what else
    // should also be reset, so for now this method creates new instances of everything,
    // which is not so cool.
    val arguments = CommandLineParser.tokenize(compilerOptions)
    val command = new CompilerCommand(arguments.toList, reportError _)
    val outputDir = new VirtualDirectory("(memory)", None)
    command.settings.outputDirs setSingleOutput outputDir
    val reporter = new TestReporter(command.settings)

    new Global(command.settings, reporter)
  }

  /**
   * Compiles the given code and returns its representation as a String.
   */
  // TODO: Returning the compiled code as a String is a huge step backwards compared
  // to using toolboxes... It would be cool to extract the tree we're interested in.
  def compile(code: String) = {
    val global = getCompiler
    import global._
    val source = new BatchSourceFile(NoFile, wrap(code))
    val run = new Run
    run.compileSources(source :: Nil)
    unWrap(global)(run.units.next.body)
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

// The code below is a huge mess that initially was an attempt at using ToolBoxes with the hijacked syntax analyzer.
// Unfortunately this didn't work out because ToolBoxes directly call `Global.newUnitParser(CompilationUnit)` which
// in turn returns a vanilla `UnitParser`, and thus bypasses the hijacking of the syntax analyzer.
// The code is left there in case a brilliant idea comes to solve this problem.
// import org.duhemm.parsermacro.ParserMacroSyntaxAnalyzer
// import scala.collection.mutable

// import scala.tools.nsc.{ Global, SubComponent }
// import scala.tools.reflect.{ ToolBox, ReflectGlobal }
// import scala.reflect.runtime.{ universe => ru }

// import scala.tools.nsc.Global
// import scala.tools.nsc.reporters.Reporter
// import scala.tools.nsc.Settings

// class MyHackedCompiler(tx: scala.tools.reflect.ToolBoxFactory$ToolBoxImpl, old: scala.tools.reflect.ToolBoxFactory$ToolBoxImpl$ToolBoxGlobal) extends scala.tools.reflect.ToolBoxFactory$ToolBoxImpl$ToolBoxGlobal(null, null, null) { //(old.frontEnd, old.options, null) {
//   self =>
//   override def newUnitParser(unit: CompilationUnit) =
//     ???
//     //(new { val global: self.type = self } with ParserMacroSyntaxAnalyzer).asInstanceOf[Nothing]
// }

// object Compiler {

//   val usePlugin = "-Xplugin:" + System.getProperty("sbt.paths.plugin.jar")
//   val classpath = "-cp " + sys.props("sbt.class.directory") + ":" + System.getProperty("sbt.paths.plugin.jar")
//   val tb = scala.reflect.runtime.currentMirror.mkToolBox(options = classpath + " " + usePlugin)

//   def hijackSyntaxAnalyzer(): Unit = {


//     tb.parse("1") // Make sure the toolbox compiler is initialized

//     val app  = tb.getClass.getDeclaredMethods.find(_.getName == "withCompilerApi").get
//     val res = app.invoke(tb)
//     val fx = res.getClass.getDeclaredFields.find(_.getName == "api$module").get
//     fx.setAccessible(true)
//     val f = fx.get(res)
//     val f2 = f.getClass.getDeclaredFields.find(_.getName == "compiler").get
//     f2.setAccessible(true)

//     val oldCompiler = f2.get(f).asInstanceOf[ReflectGlobal]
//     println(oldCompiler.getClass.getName)
//     val tx = tb.asInstanceOf[scala.tools.reflect.ToolBoxFactory$ToolBoxImpl]
//     val newCompiler = Class.forName("compiler.MyHackedCompiler").getDeclaredConstructors()(0).newInstance(tx, oldCompiler) //new MyHackedCompiler(oldCompiler)
//     f2.set(f, newCompiler)
//   }

//   hijackSyntaxAnalyzer()

//   def compile(src: String) = tb.typecheck(tb.parse(src))

// }
