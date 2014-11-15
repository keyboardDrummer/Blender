package transformations.bytecode.coreInstructions.integers

import core.transformation.{MetaObject, TransformationState}
import transformations.bytecode.{PrintByteCode, ByteCodeSkeleton}
import PrintByteCode._
import transformations.bytecode.coreInstructions.InstructionC
import transformations.javac.classes.ConstantPool

object IncrementIntegerC extends InstructionC {

  override val key: AnyRef = IntegerIncrementKey

  def integerIncrement(location: Int, amount: Int) = ByteCodeSkeleton.instruction(IntegerIncrementKey, Seq(location, amount))

  override def getInstructionStackSizeModification(constantPool: ConstantPool, instruction: MetaObject, state: TransformationState): Int = 0

  override def getInstructionByteCode(instruction: MetaObject): Seq[Byte] = {
    val arguments = ByteCodeSkeleton.getInstructionArguments(instruction)
    hexToBytes("84") ++
      byteToBytes(arguments(0)) ++
      byteToBytes(arguments(1))
  }

  override def getInstructionInAndOutputs(constantPool: ConstantPool, instruction: MetaObject, state: TransformationState) = (Seq.empty, Seq.empty)

  override def getInstructionSize(instruction: MetaObject): Int = 3

  object IntegerIncrementKey

}
