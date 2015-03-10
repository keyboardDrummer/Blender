package transformations.bytecode.coreInstructions.integers

import core.particles.{CompilationState, Contract, MetaObject}
import transformations.bytecode.PrintByteCode._
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.coreInstructions.{InstructionC, InstructionSignature}
import transformations.bytecode.simpleBytecode.ProgramTypeState
import transformations.javac.classes.ConstantPool
import transformations.types.IntTypeC

object LoadIntegerC extends InstructionC {

  override val key: AnyRef = IntegerLoad

  def load(location: Integer) = CodeAttribute.instruction(IntegerLoad, Seq(location))

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    val arguments = CodeAttribute.getInstructionArguments(instruction)
    val location = arguments(0)
    if (location > 3)
      hexToBytes("15") ++ byteToBytes(location)
    else
      byteToBytes(hexToInt("1a") + location)
  }

  override def getSignature(constantPool: ConstantPool, instruction: MetaObject, typeState: ProgramTypeState, state: CompilationState): InstructionSignature = InstructionSignature(Seq(), Seq(IntTypeC.intType))

  object IntegerLoad

  override def dependencies: Set[Contract] = super.dependencies ++ Set(IntTypeC)

  override def description: String = "Defines the load integer instruction, which loads an integer from a variable."
}
