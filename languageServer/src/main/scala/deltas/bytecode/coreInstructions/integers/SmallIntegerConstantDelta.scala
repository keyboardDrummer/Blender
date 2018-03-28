package deltas.bytecode.coreInstructions.integers

import core.language.node.Node
import core.language.{Compilation, Language}
import deltas.bytecode.PrintByteCode._
import deltas.bytecode.attributes.CodeAttributeDelta
import deltas.bytecode.coreInstructions.{InstructionInstance, InstructionSignature}
import deltas.bytecode.simpleBytecode.ProgramTypeState
import deltas.bytecode.types.IntTypeDelta

object SmallIntegerConstantDelta extends InstructionInstance {

  def integerConstant(value: Int) = {
    require (value <= 5)
    require (value >= -1)
    CodeAttributeDelta.instruction(shape, Seq(value))
  }

  override def getBytes(compilation: Compilation, instruction: Node): Seq[Byte] = {
    byteToBytes(3 + CodeAttributeDelta.getInstructionArguments(instruction).head)
  }

  override def getSignature(instruction: Node, typeState: ProgramTypeState, language: Language): InstructionSignature =
    InstructionSignature(Seq(), Seq(IntTypeDelta.intType))

  override def getInstructionSize(compilation: Compilation): Int = 1

  override def description: String = "Defines the integer constant instruction, which places an integer between -1 and 5 on the stack."

  override def grammarName = "iconst" //TODO eigenlijk heb je ook nog iconst_0 etc.. maar die zitten verbogen in deze Delta.
}