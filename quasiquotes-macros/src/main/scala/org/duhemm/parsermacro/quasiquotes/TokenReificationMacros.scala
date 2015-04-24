package scala.meta
package parsermacro

import scala.util.matching.Regex

import scala.meta.{ Tree => MetaTree }
import scala.meta.internal.quasiquotes.ReificationMacros
import scala.reflect.macros.whitebox.Context

import java.lang.reflect.Method

import org.duhemm.parsermacro.quasiquotes.TokenQuasiquoteLiftables

class TokenReificationMacros(override val c: Context) extends ReificationMacros(c) with TokenQuasiquoteLiftables {

  import c.universe.{ Tree => ReflectTree, _ }

  override def apply(args: ReflectTree*)(dialect: ReflectTree): c.Tree = expand(dialect)
  override def unapply(scrutinee: ReflectTree)(dialect: ReflectTree): c.Tree = ???

  object PlaceHolder {
    val pattern = """^\$placeholder(\d+)$""".r
    def apply(i: Int): String = s"$$placeholder$i"
    def unapply(token: Token): Option[Int] = pattern findFirstMatchIn token.code map (_.group(1).toInt)
  }

  // Extract the interesting parts of toks"..."
  lazy val q"$_($_.apply(..${parts: List[String]})).$_.$method[..$_](..$args)($_)" = c.macroApplication

  /** Removes the heading BOF and trailing EOF from a sequence of tokens */
  private def trim(toks: Vector[Token]): Vector[Token] = toks match {
    case (_: Token.BOF) +: tokens :+ (_: Token.EOF) => tokens
    case _                                => toks
  }

  /**
   * Construct a single string from all the parts of the input, and insert `$placeholderX`
   * where `X` is the number of the argument. This placeholder will then be replaced by
   * some value, or extracted from a sequence of tokens.
   */
  private def input: String =
    parts.init.zipWithIndex.map {
      case (part, i) => part + PlaceHolder(i)
    }.mkString("", "", parts.last)

  override def expand(dialectTree: c.Tree): c.Tree = {
    implicit val dialect: Dialect = dialects.Quasiquote(instantiateDialect(dialectTree))
    val tokens = trim(input.tokens)
    q"$tokens"
  }

  // Required to make use of private members (`instantiateDialect`) defined in `ReificationMacros`
  private val parentClass = classOf[ReificationMacros]
  private def getAndSetAccessible(meth: String): Method = {
    val m = parentClass.getDeclaredMethods.filter(_.getName == meth).headOption.getOrElse(parentClass.getDeclaredMethods.filter(_.getName endsWith meth).head)
    m.setAccessible(true)
    m
  }

  private val instantiateDialect: ReflectTree => Dialect = {
    val parentInstantiateDialect = getAndSetAccessible("instantiateDialect")
    (dialect: ReflectTree) => parentInstantiateDialect.invoke(this, dialect).asInstanceOf[Dialect]
  }
}
