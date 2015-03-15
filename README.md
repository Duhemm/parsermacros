# Parser Macros (WIP)

This repository holds a Scala compiler plugin that will bring parser macros to Scala. This plugin is still a work in progress.

## Building and using the plugin

To use the plugin, you will need a custom version of scalac, because parser macros will require a special syntax that differentiates their application from standard function application.

My [fork of scala/scala](https://github.com/Duhemm/scala/tree/macroparser) contains the modified parser. Simply build it, clone this repository and start sbt like this:

```
$ sbt -Dmacroparser.scala.home="[path where you built scala]"
```

You can place your macro implementations in `sandbox-macros/` and your macro clients in `sandbox-clients/`.
