object Client extends App {

  Macros.For#(x in 1 .. 10)#{
    println(x)
  }

  println("Hello, world!")

}
