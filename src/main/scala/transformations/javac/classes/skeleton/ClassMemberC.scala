package transformations.javac.classes.skeleton

import core.particles.{CompilationState, DeltaWithGrammar}
import core.particles.node.Node

trait ClassMemberC extends DeltaWithGrammar {
  def bind(state: CompilationState, clazz: ClassSignature, member: Node)
  def compile(state: CompilationState, member: Node)

  override def inject(state: CompilationState): Unit = {
    JavaClassSkeleton.getState(state).members ::= this
    super.inject(state)
  }
}
