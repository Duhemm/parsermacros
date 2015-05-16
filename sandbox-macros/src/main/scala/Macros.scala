import scala.meta._
import scala.meta.Token._
import scala.meta.dialects.Scala211
import scala.meta.internal.ast
import scala.language.experimental.macros

object ShowQuasiquotes extends App {
  import tokenquasiquotes._
  println(toks"hello, world!")
}

object A {
  object Nested {
    def miniMacro(tokens: Seq[Token]): Tree = ast.Lit.Int(5)
  }
}

object Macros {

  def For: Unit = macro forImpl
  def forImpl(it: Seq[Token], body: Seq[Token]): Tree = {

    val cleanIt = it.filterNot(_.isInstanceOf[Whitespace]).toList
    val cleanBody = body.filterNot(_.isInstanceOf[Whitespace]).toList

    val iteratee = cleanIt match {
      case (_: BOF) :: (variable: Ident) :: (_: Ident) :: (from: Literal.Int) :: (_: `.`) :: (_: `.`) :: (to: Literal.Int) :: (_: EOF) :: Nil =>
        val frm = ast.Lit.Int(from.code.toInt)
        val t = ast.Lit.Int(to.code.toInt)
        val in = ast.Term.Name(variable.code)
        (body: Term) => q"for($in <- Range($frm, $t)) { $body }"
      case _ =>
        ???
    }

    val bdy = Parse.parseTerm(Scala211)(body.toTokens)

    iteratee(bdy)
  }

  def localMacroDef(tokens: Seq[Token]): Tree = {
    val toks = tokens.filterNot(_.isInstanceOf[Whitespace]).toList

    val res = toks match {
      // BOF for ident in intlit .. intlit { ident { ident } } EOF
      case (_: BOF) :: (_: `for`) :: (iteratee: Ident) :: (in: Ident) :: (from: Literal.Int) :: (_: `.`) :: (_: `.`) :: (to: Literal.Int) :: (_: `{`) :: (pln: Ident) :: (_: `{`) :: (_: Ident) :: (_: `}`) :: (_: `}`) :: (_: EOF) :: Nil =>
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
  def miniMacro: Int = macro A.Nested.miniMacro

  def lightweight(tokens: Seq[Token]): Tree = macro {
    internal.ast.Lit.Int(tokens.length)
  }


  // Classic scala.reflect macro to make sure the plugin just ignores it.
  def classicImpl(c: scala.reflect.macros.blackbox.Context)(x: c.Tree): c.Tree = {
    import c.universe._
    Literal(Constant(2))
  }
  def classic(x: Int): Int = macro classicImpl

}
