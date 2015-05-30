package org.duhemm.parsermacro

import scala.reflect.api.Universe

trait ParserMacroSyntheticObject {

  val universe: Universe

  import universe._

  object TemporaryObject {
    /**
     * Creates the tree corresponding to a dummy object, which is annotated with
     * `ParserMacroExpansion`. This object will be replaced by the expansion of
     * the parser macro during compilation.
     */
    def apply(tree: Tree, tokens: List[String]): ModuleDef =
      q"""@_root_.org.duhemm.parsermacro.ParserMacroExpansion($tree)
          object TemporaryObject {
            val tokens = $tokens
          }"""

    /**
     * Extracts the list of arguments of a parser macro from a dummy object generated
     * during parsing of a parser macro application.
     */
    def unapply(tree: Tree): Option[List[String]] = tree match {
      case q"""object TemporaryObject {
                 val tokens = scala.collection.immutable.List(..${rawArguments: List[String]})
               }""" =>
        Some(rawArguments)

      case _ =>
        None
    }
  }

}
