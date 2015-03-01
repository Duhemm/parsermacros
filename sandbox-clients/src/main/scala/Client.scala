object Client extends App {
  Macros.iterate#(for x in 1 .. 10 {
    println{ x }
  })

  Macros.miniMacro#()

  println("Hello, world!")

}
