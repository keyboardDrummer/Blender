package deltas.bytecode.coreInstructions.integers

import core.deltas.node.{Node, NodeClass}
import core.deltas.{Compilation, Contract, Language}
import deltas.bytecode.PrintByteCode._
import deltas.bytecode.attributes.CodeAttribute
import deltas.bytecode.coreInstructions.{InstructionDelta, InstructionSignature}
import deltas.bytecode.simpleBytecode.ProgramTypeState
import deltas.bytecode.types.IntTypeC

object StoreIntegerDelta extends InstructionDelta {

  override val key = IntegerStore

  def integerStore(location: Int) = CodeAttribute.instruction(IntegerStore, Seq(location))

  override def getInstructionByteCode(instruction: Node): Seq[Byte] = {
    val arguments = CodeAttribute.getInstructionArguments(instruction)
    val location = arguments(0)
    if (location > 3)
      hexToBytes("36") ++ byteToBytes(location)
    else
      byteToBytes(hexToInt("3b") + location)
  }

  override def getSignature(instruction: Node, typeState: ProgramTypeState, language: Language): InstructionSignature =
    InstructionSignature(Seq(IntTypeC.intType), Seq())

  override def getVariableUpdates(instruction: Node, typeState: ProgramTypeState ): Map[Int, Node] =
    Map(CodeAttribute.getInstructionArguments(instruction)(0) -> IntTypeC.intType)

  object IntegerStore extends NodeClass

  override def dependencies: Set[Contract] = super.dependencies ++ Set(IntTypeC)

  override def description: String = "Defines the integer store instruction, which stores the top stack integer in a variable."

  override def grammarName = "istore" //TODO missing istore_0 etc..
}
