package macros
import scala.meta._
import scala.meta.dialects.Scala211

object ParserMacros {
  // Yes, this is overly complicated: I just want to make sure that
  // we can put whatever we want in the lightweight syntax.
  def countTokens(tokens: Tokens): Tree = macro {
    def count[T](t: List[T]): Int = t match {
      case x :: xs => 1 + count(xs)
      case Nil => 0
    }
    scala.meta.Lit(count(tokens.toList))
  }

  def multiParameter(t1: Tokens, t2: Tokens) = macro {
    scala.meta.Lit(1)
  }

  def alwaysReturnOne(t1: Tokens) = macro {
    scala.meta.Lit(1)
  }

  def compatibleParameterType(tokens: Iterable[Token]) = macro {
    scala.meta.Lit(1)
  }

  def compatibleReturnType(tokens: Tokens): Lit = macro {
    scala.meta.Lit(1)
  }

  def hasTypeParameters[T](tokens: Tokens) = macro {
    scala.meta.Lit(1)
  }

  def hasTypeParametersToo[T >: Token](tokens: Seq[T]) = macro {
    scala.meta.Lit(1)
  }

  def alsoHasTypeParameters[T <: Tree](tokens: Tokens) = macro {
    scala.meta.Lit(1)
  }

  // Wrong implementations of parser macros
  def tooManyParamLists(tokens: Tokens)(otherTokens: Tokens): Tree = ???
  def incompatibleParameterTypes(something: Int): Tree = ???
  def incompatibleParameterSeqType(tokens: List[Token]): Tree = ???
  def incompatibleReturnType(tokens: Tokens): Any = ???

}
