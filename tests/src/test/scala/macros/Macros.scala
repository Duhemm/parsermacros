package macros
import scala.meta._
import scala.meta.dialects.Scala211

object Macros {
  def countTokensImpl(tokens: Seq[Token]) = {
    val count = tokens.length
    val res = internal.ast.Lit.Int(count)
    res
  }
  def countTokens: Int = macro countTokensImpl

  def alwaysReturnOneImpl(p1: Seq[Token], p2: Seq[Token]) = {
    internal.ast.Lit.Int(1)
  }
  def alwaysReturnOne: Int = macro alwaysReturnOneImpl

  def compatibleParameterTypeImpl(tokens: Iterable[Token]): Tree = internal.ast.Lit.Int(1)
  def compatibleParameterType = macro compatibleParameterTypeImpl

  def compatibleReturnTypeImpl(tokens: Seq[Token]): internal.ast.Lit = internal.ast.Lit.Int(1)
  def compatibleReturnType = macro compatibleReturnTypeImpl

  // Wrong implementations of parser macros
  def tooManyParamLists(tokens: Seq[Token])(otherTokens: Seq[Token]): Tree = ???
  def incompatibleParameterTypes(something: Int): Tree = ???
  def incompatibleParameterSeqType(tokens: List[Token]): Tree = ???
  def incompatibleReturnType(tokens: Seq[Token]): Any = ???

}

abstract class AbstractProvider {
  def abstractImpl(tokens: Seq[Token]): Tree

  def concreteImpl(tokens: Seq[Token]): Tree =
    internal.ast.Lit.Int(1)
}

object ConcreteProvider extends AbstractProvider {

  override def abstractImpl(tokens: Seq[Token]): Tree =
    internal.ast.Lit.Int(1)
  def overrideAbstractImplFromParent: Int = macro abstractImpl

  def concreteImplInAbstractParent: Int = macro concreteImpl
}
