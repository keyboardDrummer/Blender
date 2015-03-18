package transformations.bytecode.coreInstructions

import core.particles.CompilationState
import core.particles.node.MetaObject
import transformations.bytecode.PrintByteCode
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.simpleBytecode.ProgramTypeState
import transformations.javac.classes.ConstantPool

object SwapInstruction extends InstructionC {
  object SwapKey
  def swap = CodeAttribute.instruction(SwapKey)

  override val key: AnyRef = SwapKey

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    PrintByteCode.hexToBytes("5f")
  }

  override def getSignature(instruction: MetaObject, typeState: ProgramTypeState, state: CompilationState): InstructionSignature = {
    val stackTop = typeState.stackTypes.takeRight(2)
    new InstructionSignature(stackTop, stackTop.reverse)
  }

  override def description: String = "Defines the swap instruction, which swap the top two values on the stack."
}
