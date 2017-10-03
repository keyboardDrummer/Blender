package transformations.javac.statements

import core.particles.{Language, Delta, DeltaWithGrammar}

object JavaGotoC extends Delta {

  override def inject(state: Language): Unit = {
    JustJavaGoto.inject(state)
    JustJavaLabel.inject(state)
    super.inject(state)
  }

  override def description: String = "Adds goto and label statements"
}
