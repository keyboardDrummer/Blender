package transformations.bytecode.coreInstructions

import core.particles.Compilation
import core.particles.node.{Node, NodeClass}
import transformations.bytecode.PrintByteCode
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.simpleBytecode.ProgramTypeState


object DuplicateInstructionDelta extends InstructionDelta {

  object DuplicateKey extends NodeClass
  def duplicate = CodeAttribute.instruction(DuplicateKey, Seq.empty)

  override val key = DuplicateKey

  override def getInstructionByteCode(instruction: Node): Seq[Byte] = {
    PrintByteCode.hexToBytes("59")
  }

  override def getSignature(instruction: Node, typeState: ProgramTypeState, state: Compilation): InstructionSignature = {
    val input: Node = typeState.stackTypes.last
    assertSingleWord(state, input)
    new InstructionSignature(Seq(input),Seq(input, input))
  }

  override def description: String = "Defines the duplicate instruction, which duplicates the top stack value."

  override def grammarName = "dup"
}
