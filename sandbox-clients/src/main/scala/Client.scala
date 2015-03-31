object Client extends App {

  def hello(tokens: Seq[scala.meta.Token]): scala.meta.Tree = macro {
    scala.meta.internal.ast.Lit.Int(1)
  }

  Macros.For#(x in 1 .. 10)#{
    println(x)
  }

  println(Macros.lightweight#(Hello, world!))

  println("Even better: " + Client.hello#(Hello, world!))

}
