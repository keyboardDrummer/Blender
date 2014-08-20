package transformations.bytecode.coreInstructions.integers

import core.transformation.{Contract, MetaObject, TransformationState}
import transformations.bytecode.ByteCodeSkeleton
import transformations.bytecode.PrintByteCode._
import transformations.bytecode.coreInstructions.InstructionC
import transformations.javac.classes.ConstantPool
import transformations.types.IntTypeC

object LoadIntegerC extends InstructionC {

  override val key: AnyRef = IntegerLoad

  def load(location: Integer) = ByteCodeSkeleton.instruction(IntegerLoad, Seq(location))

  override def getInstructionStackSizeModification(constantPool: ConstantPool, instruction: MetaObject, state: TransformationState): Int = 1

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    val arguments = ByteCodeSkeleton.getInstructionArguments(instruction)
    val location = arguments(0)
    if (location > 3)
      hexToBytes("15") ++ byteToBytes(location)
    else
      byteToBytes(hexToInt("1a") + location)
  }

  override def getInstructionInAndOutputs(constantPool: ConstantPool, instruction: MetaObject, state: TransformationState) = (Seq(), Seq(IntTypeC.intType))

  object IntegerLoad

  override def dependencies: Set[Contract] = super.dependencies ++ Set(IntTypeC)

}