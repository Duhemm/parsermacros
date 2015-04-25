package scala.meta
package parsermacro

import scala.util.matching.Regex

import scala.meta.{ Tree => MetaTree }
import scala.meta.internal.ast.Lit
import scala.meta.internal.quasiquotes.ReificationMacros
import scala.reflect.macros.whitebox.Context

import java.lang.reflect.Method

import org.duhemm.parsermacro.quasiquotes.TokenQuasiquoteLiftables

class TokenReificationMacros(override val c: Context) extends ReificationMacros(c) with TokenQuasiquoteLiftables {

  import c.universe.{ Tree => ReflectTree, _ }

  override def apply(args: ReflectTree*)(dialect: ReflectTree): c.Tree = expand(dialect)
  override def unapply(scrutinee: ReflectTree)(dialect: ReflectTree): c.Tree = ???

  // Extract the interesting parts of toks"..."
  lazy val q"$_($_.apply(..${parts: List[String]})).$_.$method[..$_](..$args)($_)" = c.macroApplication

  /** Removes the heading BOF and trailing EOF from a sequence of tokens */
  private def trim(toks: Vector[Token]): Vector[Token] = toks match {
    case (_: Token.BOF) +: tokens :+ (_: Token.EOF) => tokens
    case _                                => toks
  }

  /**
   * Gets the tokens for each part of the input, and glue them together in a single Vector
   * by inserting special `Token.Unquote` tokens between them.
   * The tree of the Unquote token is a scala.meta tree that represents the index of the
   * argument.
   */
  private def input(implicit dialect: Dialect): Vector[Token] =
    parts.toVector.init.zipWithIndex.flatMap {
      case (part, i) =>
        trim(part.tokens) :+ Token.Unquote(Input.None, dialect, 0, 0, 0, Lit.Int(i), Token.Prototype.None)
    } ++ trim(parts.last.tokens)


  override def expand(dialectTree: c.Tree): c.Tree = {
    implicit val dialect: Dialect = dialects.Quasiquote(instantiateDialect(dialectTree))
    input match {
      case Seq(single) => q"$single"
      case tokens      => q"$tokens"
    }
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
