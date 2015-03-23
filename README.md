# Parser Macros (WIP) [![Build Status](https://travis-ci.org/Duhemm/parsermacros.svg?branch=master)](https://travis-ci.org/Duhemm/parsermacros)

This repository holds a Scala compiler plugin that will bring parser macros to Scala. This plugin is still a work in progress.

## Building and using the plugin

To use the plugin, you will need a custom version of scalac, because parser macros will require a special syntax that differentiates their application from standard function application.

My [fork of scala/scala](https://github.com/Duhemm/scala/tree/macroparser) contains the modified parser. Simply build it, clone this repository and start sbt like this:

```
$ sbt -Dmacroparser.scala.home="[path where you built scala]"
```

You can place your macro implementations in `sandbox-macros/` and your macro clients in `sandbox-clients/`.

### Using the plugin in the Scala REPL

You will need to have built my [fork of scala/scala](https://github.com/Duhemm/scala/tree/macroparser). You can do it just [the way travis-ci does](https://github.com/Duhemm/parsermacros/blob/master/.travis.yml). You also need to build the plugin:
```
$ sbt -Dmacroparser.scala.home="[path where you build scala]" cmp
```

This will build the plugin in `plugin/target/scala-2.11/plugin_2.11-0.1.0-SNAPSHOT.jar`. To start a scala REPL with the plugin loaded, simply run:

```
$ SCALA="/where/you/cloned/scala"
$ PMACRO="/where/you/cloned/parsermacro"
$ $SCALA/build/quick/bin/scala -Xplugin:$PMACRO/plugin/target/scala-2.11/fat-plugin.jar -cp $PMACRO/plugin/target/scala-2.11/fat-plugin.jar
```

You can then start having fun with macro parsers !

```
Welcome to Scala version 2.11.6-20150213-114752-da49d9a00e (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_31).
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
