package transformations.bytecode.coreInstructions.longs

import core.particles.CompilationState
import core.particles.node.{Key, Node}
import transformations.bytecode.PrintByteCode._
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.coreInstructions.{InstructionDelta, InstructionSignature}
import transformations.bytecode.simpleBytecode.ProgramTypeState
import transformations.bytecode.types.LongTypeC

object StoreLongDelta$  extends InstructionDelta {

  override val key: Key = LongStore

  def longStore(location: Int) = CodeAttribute.instruction(LongStore, Seq(location))

  override def getInstructionByteCode(instruction: Node): Seq[Byte] = {
    val arguments = CodeAttribute.getInstructionArguments(instruction)
    val location = arguments(0)
    if (location > 3)
      hexToBytes("37") ++ byteToBytes(location)
    else
      byteToBytes(hexToInt("3f") + location)
  }

  override def getSignature(instruction: Node, typeState: ProgramTypeState, state: CompilationState): InstructionSignature = InstructionSignature(Seq(LongTypeC.longType), Seq())

  override def getVariableUpdates(instruction: Node, typeState: ProgramTypeState ): Map[Int, Node] =
    Map(CodeAttribute.getInstructionArguments(instruction)(0) -> LongTypeC.longType)

  object LongStore extends Key

}
