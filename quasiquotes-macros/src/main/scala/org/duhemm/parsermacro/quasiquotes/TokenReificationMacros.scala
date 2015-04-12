package scala.meta
package parsermacro

import scala.unchecked
import scala.meta.{ Tree => MetaTree }
import scala.meta.internal.quasiquotes.ReificationMacros
import scala.reflect.macros.whitebox.Context

import java.lang.reflect.Method

import org.duhemm.parsermacro.quasiquotes.TokenQuasiquoteLiftables

class TokenReificationMacros(override val c: Context) extends ReificationMacros(c) with TokenQuasiquoteLiftables {

  import c.universe.{ Tree => ReflectTree }

  override def apply(args: ReflectTree*)(dialect: ReflectTree): c.Tree = expand(dialect)
  override def unapply(scrutinee: ReflectTree)(dialect: ReflectTree): c.Tree = ???

  override def expand(dialect: ReflectTree): ReflectTree = {
    val (tokens, mode) = parseTokens(instantiateDialect(dialect))
    import c.universe._
    q"$tokens"
  }

  private val parentClass = classOf[ReificationMacros]
  private def getAndSetAccessible(meth: String): Method = {
    val m = parentClass.getDeclaredMethods.filter(_.getName == meth).headOption.getOrElse(parentClass.getDeclaredMethods.filter(_.getName endsWith meth).head)
    m.setAccessible(true)
    m
  }

  private def parseTokens(dialect: Dialect): (List[Token], Mode) = {
    import c.universe._
    val (parts, args, mode) =
      c.macroApplication match {
        case q"$_($_.apply(..$parts)).$_.apply[..$_](..$args)($_)" =>
          (parts, args, Mode.Term)

        case _ =>
          c.abort(c.macroApplication.pos, "Token quasiquotes can only be used for term construction at the moment.")
      }

    implicit val parsingDialect: Dialect = scala.meta.dialects.Quasiquote(dialect)
    val tokens: List[Token] = parts flatMap { case q"${part: String}" => part.tokens }

    if (tokens.length > 1) (tokens.tail.init, mode)
    else (tokens, mode)
  }

  private val instantiateDialect: ReflectTree => Dialect = {
    val parentInstantiateDialect = getAndSetAccessible("instantiateDialect")
    (dialect: ReflectTree) => parentInstantiateDialect.invoke(this, dialect).asInstanceOf[Dialect]
  }
}
