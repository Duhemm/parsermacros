import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.internal.ast
import scala.meta.syntactic.tokenizers.Token
import scala.meta.syntactic.tokenizers.Token._
import scala.language.experimental.macros

object A {
  object Nested {
    def miniMacro(tokens: Seq[Seq[Token]]): Tree = ast.Lit.Int(5)
  }
}

object Macros {

  def localMacroDef(tokens: Seq[Seq[Token]]): Tree = {
    val toks = tokens.head.filterNot(_.isInstanceOf[Whitespace]).toList

    val res = toks match {
      // for ident in intlit .. intlit { ident { ident } }
      case `for`(_, _) :: (iteratee: Ident) :: (in: Ident) :: (from: Literal.Int) :: `.`(_, _) :: `.`(_, _) :: (to: Literal.Int) :: `{`(_, _) :: (pln: Ident) :: `{`(_, _) :: (_: Ident) :: `}`(_, _) :: `}`(_, _) :: rest =>
        val in = ast.Term.Name(iteratee.code)
        val operation = ast.Term.Name(pln.code)
        val frm = ast.Lit.Int(from.code.toInt)
        val t = ast.Lit.Int(to.code.toInt)
        q"for($in <- Range($frm, $t)) $operation($in)"

      case _ =>
        q"()"
    }

    res

  }
  def iterate: Unit = macro localMacroDef
  def miniMacro: String = macro A.Nested.miniMacro


  // Classic scala.reflect macro to make sure the plugin just ignores it.
  def classicImpl(c: scala.reflect.macros.blackbox.Context)(x: c.Tree): c.Tree = {
    import c.universe._
    Literal(Constant(2))
  }
  def classic(x: Int): Int = macro classicImpl

}
