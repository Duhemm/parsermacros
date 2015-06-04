// Defined in the empty package
import scala.meta._
import scala.meta.dialects.Scala211

object InEmptyPackage {
  def foo(t: Tokens) = macro { q"1" }
}
