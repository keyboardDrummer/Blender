package transformations.bytecode.extraBooleanInstructions

import core.particles._
import core.particles.node.MetaObject
import transformations.bytecode.ByteCodeSkeleton
import transformations.bytecode.additions.LabelledTargets
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.coreInstructions.integers.IntegerConstantC
import transformations.bytecode.simpleBytecode.InferredStackFrames

import scala.collection.mutable

object ExpandInstructionsC extends ParticleWithPhase with WithState {

  def lessThanInstruction = CodeAttribute.instruction(LessThanInstructionKey)

  override def dependencies: Set[Contract] = Set(ByteCodeSkeleton)

  class State {
    val expandInstruction = new ClassRegistry[MetaObject => Seq[MetaObject]]()
  }

  override def transform(program: MetaObject, state: CompilationState): Unit = {

    val clazz = program
    val codeAnnotations: Seq[MetaObject] = CodeAttribute.getCodeAnnotations(clazz)

    for (codeAnnotation <- codeAnnotations) {
      processCodeAnnotation(codeAnnotation)
    }

    def processCodeAnnotation(codeAnnotation: MetaObject): Option[Any] = {
      val instructions = CodeAttribute.getCodeInstructions(codeAnnotation)
      val newInstructions: Seq[MetaObject] = getNewInstructions(instructions)
      codeAnnotation(CodeAttribute.CodeInstructionsKey) = newInstructions
    }

    def getNewInstructions(instructions: Seq[MetaObject]) = {

      var newInstructions = mutable.ArrayBuffer[MetaObject]()

      for (instruction <- instructions) {

        val expandOption = getState(state).expandInstruction.get(instruction.clazz)

        val replacement = instruction.clazz match {
          case LessThanInstructionKey =>
            val falseStartLabel = state.getUniqueLabel("falseStart")
            val endLabel = state.getUniqueLabel("end")
            Seq(LabelledTargets.ifIntegerCompareLess(falseStartLabel),
              IntegerConstantC.integerConstant(0),
              LabelledTargets.goTo(endLabel),
              InferredStackFrames.label(falseStartLabel),
              IntegerConstantC.integerConstant(1),
              InferredStackFrames.label(endLabel))
          case _ => Seq(instruction)
        }
        newInstructions ++= expandOption.fold(Seq(instruction))(expand => expand(instruction))
      }

      newInstructions

    }
  }

  object LessThanInstructionKey

  override def description: String = "Defines a phase where custom bytecode instructions can expand into one or several actual bytecode instructions."

  override def createState: State = new State()
}
