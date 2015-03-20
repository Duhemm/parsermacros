package org.duhemm.parsermacro

trait Exceptions { self: Plugin =>
  import global.Position

  /**
   * Exception thrown to indicate that the macro implementation doesn't have a shape that allows
   * it to be used as a parser macro.
   * Parser macro implementation should be public methods defined in a static object, accepting only
   * one parameter list. All formal parameter types should be supertypes of
   * `scala.collection.Seq[scala.meta.syntactic.Token]`, and their return type should be a subtype
   * of `scala.meta.Tree`.
   */
  private[parsermacro] case class InvalidMacroShapeException(pos: Position, msg: String) extends Exception(msg)

  /**
   * Exception thrown when the arguments that are given to a macro implementation are not subtypes of
   * `scala.collection.Seq[scala.meta.syntactic.Token]`.
   * This exception indicates that the macro implementation will not be invoked, because it would result
   * in an `InvocationTargetException`.
   */
  private[parsermacro] case class InvalidMacroInvocationException(msg: String) extends Exception(msg)

}
