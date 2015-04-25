package org.duhemm.parsermacro.quasiquotes

import org.scalameta.adt.{ Liftables => AdtLiftables }

import scala.reflect.macros.blackbox.Context
import scala.reflect.macros.Universe

import scala.meta.{ Dialect, Input, Token }
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
             def allowEllipses = ${other.allowEllipses}
           }"""
    }
  }

  implicit lazy val liftInput: Liftable[Input] = Liftable[Input] { input =>
    q"new _root_.scala.meta.Input { val content: _root_.scala.Array[_root_.scala.Char] = ${new String(input.content)}.toArray }"
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
  implicit lazy val liftToken: Liftable[Token] = Liftable {
    case Token.Unquote(_, _, _, _, _, arg: Tree, _) => arg
    case other                                      => materializeAdt[Token](other)
  }

  implicit def liftTokens: Liftable[Vector[Token]] = Liftable { tokens =>
    def prepend(tokens: Vector[Token], t: Tree): Tree =
      (tokens foldRight t) { case (token, acc) => q"$token +: $acc" }

    def append(t: Tree, tokens: Vector[Token]): Tree =
      // We call insert tokens again because there may be things that need to be spliced in it
      q"$t ++ ${insertTokens(tokens)}"

    def insertTokens(tokens: Vector[Token]): Tree = {
      val (pre, middle) = tokens span (!_.is[Token.Unquote])
      middle match {
        case Vector() =>
          prepend(pre, q"Vector.empty")
        case Token.Unquote(_, _, _, _, _, arg: Tree, _) +: rest =>
          // If we are splicing only a single token we need to wrap it in a Vector
          // to be able to append and prepend other tokens to it easily.
          val quoted = if (arg.tpe <:< typeOf[Token]) q"Vector($arg)" else arg
          append(prepend(pre, quoted), rest)
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
