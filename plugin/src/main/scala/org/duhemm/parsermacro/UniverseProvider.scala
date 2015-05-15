package org.duhemm.parsermacro

import scala.reflect.api.Universe

trait UniverseProvider {
  val universe: Universe
}
