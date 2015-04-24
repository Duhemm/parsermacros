package scala.meta

import scala.meta.parsermacro.TokenReificationMacros

object tokenquasiquotes {

  implicit class XtensionQuasiquoteTokens(val ctx: StringContext) {
    object toks {
      import scala.language.experimental.macros
      def apply(args: Any*)(implicit dialect: Dialect): Any = macro TokenReificationMacros.apply
      def unapply(scrutinee: Any)(implicit dialect: Dialect): Any = macro TokenReificationMacros.unapply
    }
  }

}
