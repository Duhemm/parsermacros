package org.duhemm.parsermacro

import scala.reflect.api.Universe

trait Signatures { self: UniverseUtils =>
  import universe._
  import compat._
  import internal.reificationSupport.setType

  // Taken from scalameta/scalahost

  // NOTE: this fixup is necessary for signatures to be accepted as annotations
  // if we don't set types in annotations, then pickler is going to crash
  // apart from constants, it doesn't really matter what types we assign, so we just go for NoType
  trait FixupSignature {
    protected def fixup(tree: Tree): Tree = {
      new Transformer {
        override def transform(tree: Tree) = {
          tree match {
            case Literal(const @ Constant(x)) if tree.tpe == null => setType(tree, ConstantType(const))
            case _ if tree.tpe == null => setType(tree, NoType)
            case _ => ;
          }
          super.transform(tree)
        }
      }.transform(tree)
    }
  }

  object LegacySignature extends FixupSignature {
    def apply(): Tree = fixup(Apply(Ident(TermName("macro")), List(Assign(Literal(Constant("macroEngine")), Literal(Constant("Parser macro experimental macro engine compatible with scala.meta APIs"))))))
  }

  object ParserMacroSignature extends FixupSignature {
    def apply(implDdef: DefDef): Tree = {
      fixup(Apply(Ident(TermName("ParserMacro")), List(
        Assign(Literal(Constant("implDdef")), implDdef))))
    }
    def unapply(tree: Tree): Option[DefDef] = {
      tree match {
        case Apply(Ident(TermName("ParserMacro")), List(
          Assign(Literal(Constant("implDdef")), (implDdef: DefDef)))) => Some(implDdef)
        case _ => None
      }
    }
  }
}
