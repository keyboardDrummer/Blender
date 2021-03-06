package miksilo.modularLanguages.core.deltas

import miksilo.languageServer.core.language.Language

import scala.collection.mutable.ArrayBuffer

case class LanguageFromDeltas(topToBottom: Seq[Delta], addMissingDeltas: Boolean = false) extends Language {
  private val explicitDeltas = topToBottom.reverse
  val allDeltas = validateDependencies(explicitDeltas)
  for(delta <- allDeltas)
  {
    delta.inject(this)
  }

  //Bad order error
  //All missing dependencies.
  def validateDependencies(explicitDeltas: Seq[Delta]): collection.Seq[Delta] = {
    var available = Set.empty[Contract]
    var allDeltas = ArrayBuffer.empty[Delta]

    def addDelta(delta: Delta, detectCycleSet: Set[Delta]): Unit = {
      delta.dependencies.foreach(dependency =>
        if (!available.contains(dependency)) {
          dependency match {
            case deltaDependency: Delta if addMissingDeltas =>
              if (detectCycleSet.contains(deltaDependency))
                throw DeltaDependencyViolation(dependency, delta)
              else
                addDelta(deltaDependency, detectCycleSet + delta)
            case _ =>
              throw DeltaDependencyViolation(dependency, delta)
          }
        }
      )
      available += delta
      allDeltas += delta
    }

    for (delta <- explicitDeltas) {
      addDelta(delta, Set.empty)
    }
    allDeltas
  }
}
