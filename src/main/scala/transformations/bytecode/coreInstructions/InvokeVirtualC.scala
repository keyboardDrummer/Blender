package transformations.bytecode.coreInstructions

import core.particles.CompilationState
import core.particles.node.{Key, Node, NodeClass}
import transformations.bytecode.PrintByteCode._
import transformations.bytecode.simpleBytecode.ProgramTypeState

object InvokeVirtualC extends InvokeC {

  override val key: Key = InvokeVirtual

  def invokeVirtual(methodRefIndex: Any) = InvokeVirtual.create(MethodRef -> methodRefIndex)

  override def getInstructionSize: Int = 3
  override def getInstructionByteCode(instruction: Node): Seq[Byte] = {
    hexToBytes("b6") ++ shortToBytes(instruction(MethodRef).asInstanceOf[Int])
  }

  override def getSignature(instruction: Node, typeState: ProgramTypeState, state: CompilationState): InstructionSignature = {
    getInstanceInstructionSignature(instruction, typeState, state)
  }

  object InvokeVirtual extends NodeClass

  override def description: String = "Defines the invoke virtual instruction, which can be used to call virtual methods."
}
