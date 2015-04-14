package scala.meta

import scala.meta.parsermacro.quasiquote

object tokenquasiquotes {

  implicit def parseTokens(implicit dialect: Dialect): Parse[Vector[Token]] = Parse.apply { _.tokens }

  @quasiquote[Vector[Token]]('toks) implicit class XtensionQuasiquoteTokens(ctx: StringContext)

}
