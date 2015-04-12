package scala.meta

import scala.meta.parsermacro.quasiquote

object tokenquasiquotes {

  implicit def parseTokens(implicit dialect: Dialect): Parse[List[Token]] = Parse.apply { _.tokens.toList }

  @quasiquote[List[Token]]('toks) implicit class XtensionQuasiquoteTokens(ctx: StringContext)

}
