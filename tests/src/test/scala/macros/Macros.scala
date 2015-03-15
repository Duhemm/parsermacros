package macros
import scala.meta._
import scala.meta.dialects.Scala211

object Macros {
  def impl(tokens: Seq[Seq[Token]]) = {
    val count = tokens.map(_.length).sum
    val res = internal.ast.Lit.Int(count)
    res
  }

  def countTokens: Int = macro impl
}
