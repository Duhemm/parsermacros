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
   * Creates a MacroRuntime for a parser macro using the legacy syntax, using the informations
   * stored in `binding`.
   */
  def legacyRuntime(binding: MacroImplBinding): Option[MacroRuntime] =
    loader.findMethod(binding.className, binding.methName) map {
      case (instance, method) =>
        (x: MacroArgs) => loader.invoke(instance, method, x.others.asInstanceOf[Seq[AnyRef]])
    }

  /**
   * Creates a MacroRuntime for a parser macro using the new scala.meta syntax, using the informations
   * stored in `binding`.
   */
  def scalametaRuntime(binding: DefDef): Option[MacroRuntime] = {

      /**
       * Computes the correct name for the object that holds the implementation we're looking for.
       */
      def erasedName(sym: Symbol): String = {
        def rootOrEmpty(sym: Symbol) = sym.isEmptyPackageClass || sym.isRoot
        sym.ownersIterator.takeWhile(!rootOrEmpty(_)).toList.reverse.partition(_.hasPackageFlag) match {
          case (Nil, objs)  => objs.map(_.name).mkString("", "$", "$")
          case (pkgs, objs) => pkgs.map(_.name).mkString(".") + (objs.map(_.name).mkString(".", "$", "$"))
        }
      }

      val runtime =
        (x: MacroArgs) => {
          val context = x.c
          val className = erasedName(context.macroApplication.symbol.owner)
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

  private val loader = new ClassLoaderProvider {
    override def withClassLoader[T](op: ClassLoader => T): T = {
      val classpath = global.classPath.asURLs
      val classloader = ScalaClassLoader.fromURLs(classpath, self.getClass.getClassLoader)

      try op(classloader)
      catch {
        case ex: ClassNotFoundException =>
          val virtualDirectory = globalSettings.outputDirs.getSingleOutput.get
          val newLoader = new AbstractFileClassLoader(virtualDirectory, classloader) {}
          op(newLoader)
      }
    }
  }

}
