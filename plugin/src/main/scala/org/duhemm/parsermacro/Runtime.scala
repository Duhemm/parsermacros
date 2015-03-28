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
   * Finds a classloader and then gives it to `op`.
   */
  private def withClassLoader[T](op: ClassLoader => T): T = {
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

  /**
   * Finds method `method` in class `clazz`. Returns an instance of `clazz` and the `java.lang.reflect.Method`
   * that represents `method`.
   */
  private def findMethod(clazz: String, method: String): Option[(Object, Method)] = withClassLoader { cl =>
    val clss = Class.forName(clazz, true, cl)
    clss.getDeclaredMethods find (_.getName == method) map { m =>
      (ReflectionUtils.staticSingletonInstance(cl, clazz), m)
    }
  }

  /**
   * Invokes `method` on `instance` with parameters `args`.
   */
  private def invoke(instance: Object, method: Method, args: MacroArgs) = {
    val arguments = args.others.asInstanceOf[Seq[AnyRef]]

    // Make sure that the parser macro is given exactly as many arguments as it expects
    if (arguments.length != method.getParameterTypes.length) {
      throw InvalidMacroInvocationException(s"parser macro expected ${method.getParameterTypes.length} but got ${arguments.length} arguments.")
    } else method.invoke(instance, arguments: _*)
  }

  /**
   * Creates a MacroRuntime for a parser macro using the legacy syntax, using the informations
   * stored in `binding`.
   */
  def legacyRuntime(binding: MacroImplBinding): Option[MacroRuntime] =
    findMethod(binding.className, binding.methName) map {
      case (instance, method) =>
        (x: MacroArgs) => invoke(instance, method, x)
    }

  /**
   * Creates a MacroRuntime for a parser macro using the new scala.meta syntax, using the informations
   * stored in `binding`.
   */
  def scalametaRuntime(binding: DefDef): Option[MacroRuntime] = {
      val runtime =
        (x: MacroArgs) => {
          val context = x.c
          val className = context.macroApplication.symbol.owner.fullName + "$"
          val methName = binding.name.toString + "$impl"

          findMethod(className, methName) map {
            case (instance, method) =>
              method.setAccessible(true)
              invoke(instance, method, x)
          } getOrElse {
            throw InvalidMacroInvocationException(s"Could not find method $methName in class $className.")
          }
        }

      Some(runtime)
    }
}
