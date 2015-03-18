package transformations.bytecode.coreInstructions

import core.particles.CompilationState
import core.particles.node.MetaObject
import transformations.bytecode.PrintByteCode
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.attributes.CodeAttribute.JumpBehavior
import transformations.bytecode.simpleBytecode.ProgramTypeState
import transformations.javac.classes.ConstantPool

object VoidReturnInstructionC extends InstructionC {

  override val key: AnyRef = VoidReturn

  def voidReturn: MetaObject = CodeAttribute.instruction(VoidReturn)

  override def jumpBehavior: JumpBehavior = new JumpBehavior(false, false)

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = PrintByteCode.hexToBytes("b1")

  override def getSignature(instruction: MetaObject, typeState: ProgramTypeState, state: CompilationState): InstructionSignature =
    InstructionSignature(Seq(), Seq())

  override def getInstructionSize: Int = 1

  object VoidReturn

  override def description: String = "Defines the void return instruction, which returns from the current method."
}
