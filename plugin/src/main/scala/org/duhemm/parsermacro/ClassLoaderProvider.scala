package org.duhemm.parsermacro

import java.lang.reflect.Method

import scala.tools.nsc.Global

import scala.reflect.internal.util.ScalaClassLoader
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.runtime.ReflectionUtils

import scala.reflect.io.AbstractFile

/**
 * Base trait for classes that are able to provide a class loader to their clients.
 */
trait ClassLoaderProvider {

  /**
   * Finds a classloader and then gives it to `op`.
   */
  def withClassLoader[T](op: ClassLoader => T): T

  /**
   * Finds method `method` in class `clazz`. Returns an instance of `clazz` and the `java.lang.reflect.Method`
   * that represents `method`.
   */
  def findMethod(clazz: String, method: String): Option[(Object, Method)] = withClassLoader { cl =>
    val clss = Class.forName(clazz, true, cl)
    clss.getDeclaredMethods find (_.getName == method) map { m =>
      if (!m.isAccessible) m.setAccessible(true)
      (ReflectionUtils.staticSingletonInstance(cl, clazz), m)
    }
  }

  /**
   * Invokes `method` on `instance` with parameters `args`.
   */
  def invoke(instance: Object, method: Method, arguments: Seq[AnyRef]) = {
    // Make sure that the parser macro is given exactly as many arguments as it expects
    if (arguments.length != method.getParameterTypes.length) {
      throw InvalidMacroInvocationException(s"parser macro expected ${method.getParameterTypes.length} but got ${arguments.length} arguments.")
    } else method.invoke(instance, arguments: _*)
  }

}

class GlobalClassLoaderProvider(global: Global) extends ClassLoaderProvider {
  override def withClassLoader[T](op: ClassLoader => T): T = {
    val classpath = global.classPath.asURLs
    val classloader = ScalaClassLoader.fromURLs(classpath, this.getClass.getClassLoader)

    try op(classloader)
    catch {
      case ex: ClassNotFoundException =>
        val virtualDirectory = global.settings.outputDirs.getSingleOutput.get
        val newLoader = new AbstractFileClassLoader(virtualDirectory, classloader) {}
        op(newLoader)
    }
  }
}
