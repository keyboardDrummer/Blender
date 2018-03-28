package deltas.javac.statements.locals

import core.language.node.Node
import core.deltas.path.NodePath
import core.language.Compilation
import deltas.javac.methods.VariablePool
import deltas.javac.statements.{StatementFlowAnalysis, StatementSkeleton}

class LocalsAnalysis(compilation: Compilation, method: Node)
  extends StatementFlowAnalysis[VariablePool](compilation, method) {

  override def updateState(state: VariablePool, node: NodePath): VariablePool = {
    val instances = StatementSkeleton.instances.get(compilation)
    var newState = state
    for(entry <- instances(node.shape).definedVariables(compilation, node.current))
      newState = newState.add(entry._1,entry._2)
    newState
  }

  override def combineState(first: VariablePool, second: VariablePool): Option[VariablePool] = {
    if (first.typedVariables.keys == second.typedVariables.keys)
      return None

    Some(VariablePool(first.language, first.typedVariables.filterKeys(second.typedVariables.contains)))
  }
}