package transformations.bytecode.simpleBytecode

import core.particles.node.{Node, NodeClass}
import transformations.bytecode.additions.LabelledLocations
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.attributes.CodeAttribute.JumpBehavior
import util.DataFlowAnalysis

abstract class InstructionFlowAnalysis[State](instructions: Seq[Node])
  extends DataFlowAnalysis[Int, State] {

  def getJumpBehavior(instructionClazz: NodeClass): JumpBehavior
  
  val labelIndices = instructions.zipWithIndex.
    filter(indexedInstruction => indexedInstruction._1.clazz == LabelledLocations.LabelKey).
    map(indexedInstruction => (LabelledLocations.getLabelName(indexedInstruction._1), indexedInstruction._2)).toMap

  override def getOutgoingNodes(instructionIndex: Int): Set[Int] = {
    val instruction = instructions(instructionIndex)

    val jumpBehavior = getJumpBehavior(instruction.clazz)
    var result = Set.empty[Int]
    if (jumpBehavior.movesToNext)
      result += instructionIndex + 1

    if (jumpBehavior.hasJumpInFirstArgument)
      result += labelIndices(CodeAttribute.getInstructionArguments(instruction).head.asInstanceOf[String])

    result
  }
}
