import scala.meta.syntactic.tokenizers.Token
import scala.meta.internal.ast._

object Macros {
  def foo(tokens: Seq[Seq[Token]]): Tree = {
    val length = tokens.map(_.length).sum
    Lit.Int(length)
  }

  def bar: Int = macro foo
}
