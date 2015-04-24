package org.duhemm.parsermacro.quasiquotes

import org.scalameta.adt.{ Liftables => AdtLiftables }

import scala.reflect.macros.blackbox.Context
import scala.reflect.macros.Universe

import scala.meta.{ Dialect, Input, Token }

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
   * Liftable for `Token`. If this token represents a placeholder that is, if it is a token
   * introduced to fill a hole in a token quasiquote, then we will simply lift it as its actual
   * value (that is, the value given to the token quasiquote). Otherwise we rely on scala.meta's
   * Liftable infrastructure.
   */
  implicit lazy val liftToken: Liftable[Token] = Liftable {
    case PlaceHolder(i) => args(i)
    case other          => materializeAdt[Token](other)
  }

  // This liftable is here only because it is required by the Liftables infrastructure.
  // (Unquote has a member of type `Any`)
  private[quasiquotes] implicit lazy val liftAny: Liftable[Any] = Liftable[Any] { any =>
    ???
  }

}
