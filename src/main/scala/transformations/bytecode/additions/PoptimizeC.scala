package transformations.bytecode.additions

import core.particles.node.Node
import core.particles.{CompilationState, Contract, DeltaWithPhase}
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.coreInstructions._
import transformations.bytecode.simpleBytecode.InstructionTypeAnalysisFromState
import transformations.bytecode.{ByteCodeMethodInfo, ByteCodeSkeleton}
import transformations.bytecode.types.TypeSkeleton

object PoptimizeC extends DeltaWithPhase {

  override def dependencies: Set[Contract] = Set(PopDelta)

  private def getSignatureInOutLengths(state: CompilationState, signature: InstructionSignature): (Int, Int) = {
    val inputLength = signature.inputs.map(_type => TypeSkeleton.getTypeSize(_type, state)).sum
    val outputLength = signature.outputs.map(_type => TypeSkeleton.getTypeSize(_type, state)).sum
    (inputLength, outputLength)
  }

  override def transform(clazz: Node, state: CompilationState): Unit = {
    for (method <- ByteCodeSkeleton.getMethods(clazz)) {
      val typeAnalysis = new InstructionTypeAnalysisFromState(state, method)
      val codeAnnotation = ByteCodeMethodInfo.getMethodAttributes(method).find(a => a.clazz == CodeAttribute.CodeKey).get
      val instructions = CodeAttribute.getCodeInstructions(codeAnnotation)

      def getInOutSizes(instructionIndex: Int) = {
        val instruction = instructions(instructionIndex)
        val signatureProvider = CodeAttribute.getInstructionSignatureRegistry(state)(instruction.clazz)
        val signature = signatureProvider.getSignature(instruction, typeAnalysis.typeStatePerInstruction(instructionIndex), state)
        getSignatureInOutLengths(state, signature)
      }

      var newInstructions = List.empty[Node]
      var consumptions = List.empty[Boolean]

      def processInstruction(instructionIndex: Int) {
        val instruction = instructions(instructionIndex)
        if (instruction.clazz == PopDelta.PopKey) {
          consumptions ::= true
          return
        }

        if (instruction.clazz == Pop2Delta.Pop2Key) {
          consumptions = List(true,true) ++ consumptions
          return
        }

        val (in, out) = getInOutSizes(instructionIndex)
        var outLeft = out
        var outPop = 0
        while (outLeft > 0 && consumptions.nonEmpty) {
          val pop = consumptions.head
          outPop += (if (pop) 1 else 0)
          consumptions = consumptions.tail
          outLeft -= 1
        }
        val outConsumption = out - outPop
        val hasSideEffect = guessIfInstructionHasSideEffect(out)
        val keepInstruction = outConsumption != 0 || hasSideEffect
        if (keepInstruction) {
          val pop2Instructions = 0.until(outPop / 2).map(_ => Pop2Delta.pop2).toList
          val pop1Instructions: ((Nothing) => Any) with Iterable[Node] = if (outPop % 2 == 1) Seq(PopDelta.pop) else Set.empty
          newInstructions = pop2Instructions ++ pop1Instructions ++ newInstructions
          consumptions = 0.until(in).map(_ => false).toList ++ consumptions
          newInstructions = instruction :: newInstructions
        }
      }

      for (instructionIndex <- instructions.indices.reverse) {
        processInstruction(instructionIndex)
      }
      codeAnnotation(CodeAttribute.CodeInstructionsKey) = newInstructions.toSeq
    }
  }

  def guessIfInstructionHasSideEffect(out: Int): Boolean = {
    out == 0 //dangerous assumption :D
  }

  override def description: String = "Optimizes a bytecode program by removing instructions in cases where an instructions output will always be consumed by a pop."
}
