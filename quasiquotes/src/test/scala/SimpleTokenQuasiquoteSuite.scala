import scala.meta.dialects.Scala211
import scala.meta.tokenquasiquotes._

import scala.meta.Token._

class SimpleTokenQuasiquoteSuite extends TokenQuasiquoteSuite {
  test("`hello world` is correctly tokenized") {
    toks"hello world" shouldConformTo (
      _ isIdentNamed "hello",
      _.isWhitespace,
      _ isIdentNamed "world"
    )
  }

  test("`hello, world!` is correctly tokenized") {
    toks"hello, world!".stripWhitespaces shouldConformTo (
      _ isIdentNamed "hello",
      _.isComma,
      _ isIdentNamed "world",
      _ isIdentNamed "!"
    )
  }

  test("Integers should be correctly tokenized") {
    toks"123" shouldConformTo (
      _ isIntValued 123
    )
  }

  test("Negative integers should be correctly tokenized") {
    toks"-123" shouldConformTo (
      _.isMinus,
      _ isIntValued -123
    )
  }

  test("Single token splicing should work") {
    val hola  = toks"hello".head
    val mundo = toks"world".head

    toks"$hola, $mundo!".stripWhitespaces shouldConformTo (
      _ isIdentNamed "hello",
      _.isComma,
      _ isIdentNamed "world",
      _ isIdentNamed "!"
    )
  }
}
