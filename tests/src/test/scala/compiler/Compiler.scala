package compiler

import scala.tools.reflect.ToolBox
import scala.reflect.runtime.{universe => ru}
import scala.meta._
import scala.meta.dialects.Scala211

object Compiler {

  val usePlugin = "-Xplugin:" + System.getProperty("sbt.paths.plugin.jar")
  val classpath = "-cp " + sys.props("sbt.class.directory") + ":" + System.getProperty("sbt.paths.plugin.jar")
  val tb = scala.reflect.runtime.currentMirror.mkToolBox(options = classpath + " " + usePlugin)

  def compile(src: String) = tb.typecheck(tb.parse(src))

}
