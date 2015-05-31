package org.duhemm.parsermacro

import java.lang.reflect.Method
import scala.reflect.internal.util.ScalaClassLoader
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.runtime.ReflectionUtils

import scala.reflect.io.AbstractFile

trait Runtime { self: Plugin =>

  import global._
  import analyzer._

  /**
   * Creates a MacroRuntime for a parser macro.
   */
  def parserMacroRuntime(binding: DefDef): Option[MacroRuntime] = {
      val runtime =
        (x: MacroArgs) => {
          val context = x.c
          val className = erasedName(binding.symbol.owner)
          val methName = binding.name.toString + "$impl"

          loader.findMethod(className, methName) map {
            case (instance, method) =>
              loader.invoke(instance, method, x.others.asInstanceOf[Seq[AnyRef]])
          } getOrElse {
            throw InvalidMacroInvocationException(s"Could not find method $methName in class $className.")
          }
        }

      Some(runtime)
  }

  private lazy val loader = new GlobalClassLoaderProvider(global)

}
