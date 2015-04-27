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
    val hola  = toks"hello"
    val mundo = toks"world"

    toks"$hola, $mundo!".stripWhitespaces shouldConformTo (
      _ isIdentNamed "hello",
      _.isComma,
      _ isIdentNamed "world",
      _ isIdentNamed "!"
    )
  }

  test("Multiple token splicing should work") {
    val insertMe = toks"ipsum dolor sit"

    insertMe.stripWhitespaces shouldConformTo (
      _ isIdentNamed "ipsum",
      _ isIdentNamed "dolor",
      _ isIdentNamed "sit"
    )

    toks"lorem $insertMe amet".stripWhitespaces shouldConformTo (
      _ isIdentNamed "lorem",
      _ isIdentNamed "ipsum",
      _ isIdentNamed "dolor",
      _ isIdentNamed "sit",
      _ isIdentNamed "amet"
    )
  }

  test("Splicing multiple tokens at multiple locations") {
    val insertMe1 = toks"My first program"
    val insertMe2 = toks"hello world"

    toks"$insertMe1 said $insertMe2".stripWhitespaces shouldConformTo (
      _ isIdentNamed "My",
      _ isIdentNamed "first",
      _ isIdentNamed "program",
      _ isIdentNamed "said",
      _ isIdentNamed "hello",
      _ isIdentNamed "world"
    )
  }

  test("Token quasiquotes inside token quasiquotes") {
    toks"""token quasiquotes ${toks"are awesome"}!""".stripWhitespaces shouldConformTo (
      _ isIdentNamed "token",
      _ isIdentNamed "quasiquotes",
      _ isIdentNamed "are",
      _ isIdentNamed "awesome",
      _ isIdentNamed "!"
    )
  }

  test("Simple pattern matching should work") {
    val toks"hello world" = toks"hello world"
  }

  test("Pattern matching should only match actually matching tokens") {
    toks"hello world" match {
      case toks"hola mundo" => fail("Should not have matched this")
      case toks"hello world" => ()
    }
  }

  test("Pattern extraction should world") {
    val toks"$hello $world" = toks"hola mundo"
    assert(hello isIdentNamed "hola")
    assert(world isIdentNamed "mundo")
  }

  test("Pattern extraction with types specified") {
    import scala.meta.Token
    val toks"${hello: Token.Ident} ${world: Token.Ident} ${number: Token.Literal.Int}" = toks"hola mundo 123"
    assert(hello isIdentNamed "hola")
    assert(world isIdentNamed "mundo")
    assert(number isIntValued 123)
  }

}
