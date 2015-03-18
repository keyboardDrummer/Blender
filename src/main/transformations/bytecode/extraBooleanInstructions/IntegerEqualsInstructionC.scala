package transformations.bytecode.extraBooleanInstructions

import core.particles.node.MetaObject
import core.particles.{CompilationState, Contract}
import transformations.bytecode.additions.LabelledTargets
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.coreInstructions.integers.IntegerConstantC
import transformations.bytecode.coreInstructions.integers.integerCompare.IfIntegerCompareEqualC
import transformations.bytecode.simpleBytecode.InferredStackFrames

object IntegerEqualsInstructionC extends ExpandInstruction {

  def equals = CodeAttribute.instruction(IntegerEqualsInstructionKey)

  override def dependencies: Set[Contract] = super.dependencies ++ Set(LabelledTargets, IfIntegerCompareEqualC)

  override def key: Any = IntegerEqualsInstructionKey

  override def expand(instruction: MetaObject, state: CompilationState): Seq[MetaObject] = {
    val falseStartLabel = state.getUniqueLabel("falseStart")
    val endLabel = state.getUniqueLabel("end")
    Seq(LabelledTargets.ifIntegerCompareEquals(falseStartLabel),
      IntegerConstantC.integerConstant(0),
      LabelledTargets.goTo(endLabel),
      InferredStackFrames.label(falseStartLabel),
      IntegerConstantC.integerConstant(1),
      InferredStackFrames.label(endLabel))
  }

  object IntegerEqualsInstructionKey

  override def description: String = "Defines a custom instruction which applies == to the top stack values."
}
