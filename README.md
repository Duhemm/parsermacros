# Parser Macros (WIP) [![Build Status](https://travis-ci.org/Duhemm/parsermacros.svg?branch=master)](https://travis-ci.org/Duhemm/parsermacros)

This repository holds a Scala compiler plugin that will bring parser macros to Scala. This plugin is still a work in progress.

## Building and using the plugin

The plugin is not yet published anywhere. You will need to clone this repository and build the plugin yourself. Fortunately, it's pretty straightforward to do using sbt:

```
$ git clone git@github.com:Duhemm/parsermacro.git
$ cd parsermacro
$ sbt cmp test
```

You can place your macro implementations in `sandbox-macros/` and your macro clients in `sandbox-clients/`.

### Using the plugin in the Scala REPL

This plugin can also be used directly in the Scala REPL after you've built it. To build a self-contained JAR that includes this plugin among with its dependencies ([scala.meta](https://github.com/scalameta/scalameta)), the best is to use sbt:

```
$ sbt cmp
```

This will produce a fat JAR in `plugin/target/scala-2.11/fat-plugin.jar`. To start a Scala REPL with the plugin loaded, simply run:

```
$ scala -Xplugin:plugin/target/scala-2.11/fat-plugin.jar -cp plugin/target/scala-2.11/fat-plugin.jar
```

You can then start having fun with parser macros !

```scala
Welcome to Scala version 2.11.6 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_31).
Type in expressions to have them evaluated.
Type :help for more information.

scala> import scala.meta._
import scala.meta._

scala> def impl(t: Seq[Token]): Tree = internal.ast.Lit.Int(t.size)
impl: (t: Seq[meta.Token])scala.meta.Tree

scala> def count: Int = macro impl
defined term macro count: Int

scala> count#(Hello, world!)
res0: Int = 7
```
