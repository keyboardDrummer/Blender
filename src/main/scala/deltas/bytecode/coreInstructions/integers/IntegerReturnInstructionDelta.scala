package deltas.bytecode.coreInstructions.integers

import core.deltas.node.{Node, NodeClass}
import core.deltas.{Compilation, Contract, Language}
import deltas.bytecode.PrintByteCode
import deltas.bytecode.attributes.CodeAttribute
import deltas.bytecode.attributes.CodeAttribute.JumpBehavior
import deltas.bytecode.coreInstructions.{InstructionDelta, InstructionSignature}
import deltas.bytecode.simpleBytecode.ProgramTypeState
import deltas.bytecode.types.IntTypeC

object IntegerReturnInstructionDelta extends InstructionDelta {

  override val key = IntegerReturn

  def integerReturn: Node = CodeAttribute.instruction(IntegerReturn)

  override def jumpBehavior: JumpBehavior = new JumpBehavior(false, false)

  override def getInstructionSize: Int = 1

  override def getSignature(instruction: Node, typeState: ProgramTypeState, language: Language): InstructionSignature = InstructionSignature(Seq(IntTypeC.intType), Seq())

  override def getInstructionByteCode(instruction: Node): Seq[Byte] = PrintByteCode.hexToBytes("ac")

  object IntegerReturn extends NodeClass

  override def dependencies: Set[Contract] = super.dependencies ++ Set(IntTypeC)

  override def description: String = "Defines the integer return instruction, which returns an integer from the current method."

  override def grammarName = "ireturn"
}
