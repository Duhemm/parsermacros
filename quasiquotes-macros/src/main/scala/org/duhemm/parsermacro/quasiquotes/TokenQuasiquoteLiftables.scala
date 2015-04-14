package org.duhemm.parsermacro.quasiquotes

import org.scalameta.adt.{ Liftables => AdtLiftables }

import scala.reflect.macros.blackbox.Context

import scala.meta.{ Dialect, Input, Token }

trait TokenQuasiquoteLiftables extends AdtLiftables {
  val u: scala.reflect.macros.Universe

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
    q"new _root_.scala.meta.Input { val content: _root_.scala.Array[_root_.scala.Char] = ${input.content} }"
  }

  implicit lazy val liftBool2Double: Liftable[Boolean => Double] = Liftable[Boolean => Double] { f =>
    q"(x: _root_.scala.Boolean) => if (x) ${f(true)} else ${f(false)}"
  }

  implicit lazy val liftBool2Int: Liftable[Boolean => Int] = Liftable[Boolean => Int] { f =>
    q"(x: _root_.scala.Boolean) => if (x) ${f(true)} else ${f(false)}"
  }

  implicit lazy val liftBool2Float: Liftable[Boolean => Float] = Liftable[Boolean => Float] { f =>
    q"(x: _root_.scala.Boolean) => if (x) ${f(true)} else ${f(false)}"
  }

  implicit lazy val liftBool2Long: Liftable[Boolean => Long] = Liftable[Boolean => Long] { f =>
    q"(x: _root_.scala.Boolean) => if (x) ${f(true)} else ${f(false)}"
  }

  implicit def liftToken: Liftable[Token] = materializeAdt[Token]

  // This liftable is here only because it is required by the Liftables infrastructure.
  // (Unquote has a member of type `Any`)
  private[quasiquotes] implicit lazy val liftAny: Liftable[Any] = Liftable[Any] { any =>
    ???
  }

}