package transformations.javac.constructor

import core.transformation.sillyCodePieces.ParticleWithPhase
import core.transformation.{Contract, MetaObject, TransformationState}
import transformations.javac.methods.MethodC
import transformations.javac.statements.ExpressionAsStatementC

object ImplicitSuperConstructorCall extends ParticleWithPhase {
  override def dependencies: Set[Contract] = Set(ConstructorC)

  override def transform(clazz: MetaObject, state: TransformationState): Unit = {

    for (constructor <- ConstructorC.getConstructors(clazz)) {
      val statements = MethodC.getMethodBody(constructor)
      var addSuperCall = false
      if (statements.isEmpty)
        addSuperCall = true
      else {
        val firstStatement = statements(0)
        if (firstStatement.clazz != SuperCallExpression.SuperCall && firstStatement.clazz != ThisCallExpression.ThisCall) {
          addSuperCall = true
        }
      }

      if (addSuperCall)
        constructor(MethodC.MethodBodyKey) = Seq(ExpressionAsStatementC.asStatement(SuperCallExpression.superCall())) ++ statements
    }
  }
}
