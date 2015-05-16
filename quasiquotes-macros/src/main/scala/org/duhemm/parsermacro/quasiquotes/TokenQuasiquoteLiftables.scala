package org.duhemm.parsermacro.quasiquotes

import org.scalameta.adt.{ Liftables => AdtLiftables }

import scala.reflect.macros.blackbox.Context
import scala.reflect.macros.Universe

import scala.meta.{ Dialect, Input, Token, Tokens }
import scala.meta.syntactic.Content
import scala.meta.internal.ast.Lit

import scala.meta.parsermacro.TokenReificationMacros

trait TokenQuasiquoteLiftables extends AdtLiftables { self: TokenReificationMacros =>

  import u._

  implicit lazy val liftDialect: Liftable[Dialect] = Liftable[Dialect] { dialect =>
    dialect match {
      case scala.meta.dialects.Scala211 => q"_root_.scala.meta.dialects.Scala211"
      case scala.meta.dialects.Dotty    => q"_root_.scala.meta.dialects.Dotty"
      case other =>
        q"""new _root_.scala.meta.Dialect {
             override def toString = ${other.toString}
             def bindToSeqWildcardDesignator = ${other.bindToSeqWildcardDesignator}
             def allowXmlLiterals = ${other.allowXmlLiterals}
             def allowEllipses = ${other.allowEllipses}
           }"""
    }
  }

  implicit lazy val liftContent: Liftable[Content] = Liftable[Content] { content =>
    q"_root_.scala.meta.Input.String(${new String(content.chars)})"
  }

  implicit def liftBool2T[T: Liftable]: Liftable[Boolean => T] = Liftable[Boolean => T] { f =>
    q"(x: _root_.scala.Boolean) => if (x) ${f(true)} else ${f(false)}"
  }

  /**
   * Liftable for `Token`. `Token.Unquote` is only inserted to glue together different parts of
   * input. For instance in `toks"foo $bar foo"`, the input will consist of 2 parts and one argument.
   * The two parts will be glued by a `Token.Unquote` token, which will be replaced here by the value
   * of the argument when the tokens get lifted.
   */
  implicit lazy val liftToken: Liftable[Token] = Liftable[Token] {
    case Token.Unquote(_, _, _, _, tree: Tree) => tree
    case other                              => materializeAdt[Token](other)
  }

  implicit def liftTokens: Liftable[Tokens] = Liftable[Tokens] { tokens =>
    def prepend(tokens: Tokens, t: Tree): Tree =
      (tokens foldRight t) { case (token, acc) => q"$token +: $acc" }

    def append(t: Tree, tokens: Tokens): Tree =
      // We call insert tokens again because there may be things that need to be spliced in it
      q"$t ++ ${insertTokens(tokens)}"

    def insertTokens(tokens: Tokens): Tree = {
      val (pre, middle) = tokens span (!_.isInstanceOf[Token.Unquote])
      middle match {
        case Tokens() =>
          prepend(pre, q"_root_.scala.meta.syntactic.Tokens()")
        case Token.Unquote(_, _, _, _, tree: Tree) +: rest =>
          // If we are splicing only a single token we need to wrap it in a Vector
          // to be able to append and prepend other tokens to it easily.
          val quoted = if (tree.tpe <:< typeOf[Token]) q"_root_.scala.meta.syntactic.Tokens($tree)" else tree
          append(prepend(pre, quoted), Tokens(rest: _*))
      }
    }

    insertTokens(tokens)
  }

  // This liftable is here only because it is required by the Liftables infrastructure.
  // (Unquote has a member of type `Any`)
  private[quasiquotes] implicit lazy val liftAny: Liftable[Any] = Liftable[Any] { any =>
    c.abort(c.macroApplication.pos, "Internal error in token quasiquote expansion: Should not try to lift scala.Any.")
  }

}
