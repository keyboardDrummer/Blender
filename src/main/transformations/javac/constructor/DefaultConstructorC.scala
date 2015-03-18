package transformations.javac.constructor

import core.particles.node.MetaObject
import core.particles.{CompilationState, Contract, ParticleWithPhase}
import transformations.javac.classes.JavaClassSkeleton
import transformations.javac.methods.MethodC.PublicVisibility

object DefaultConstructorC extends ParticleWithPhase {
  override def dependencies: Set[Contract] = Set(ConstructorC)

  def transform(clazz: MetaObject, state: CompilationState): Unit = {
    val className = JavaClassSkeleton.getClassName(clazz)
    val members = JavaClassSkeleton.getMembers(clazz)
    val constructor = ConstructorC.constructor(className, Seq(), Seq(), PublicVisibility)
    clazz(JavaClassSkeleton.Members) = Seq(constructor) ++ members
  }

  override def description: String = "Adds a default public constructor to a class if none is specified."
}
