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
}
