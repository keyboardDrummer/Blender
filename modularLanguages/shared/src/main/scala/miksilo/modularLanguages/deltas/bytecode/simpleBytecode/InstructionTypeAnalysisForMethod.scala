package miksilo.modularLanguages.deltas.bytecode.simpleBytecode

import miksilo.modularLanguages.core.node.Node
import miksilo.languageServer.core.language.Language
import miksilo.modularLanguages.deltas.bytecode.ByteCodeMethodInfo.MethodInfo
import miksilo.modularLanguages.deltas.bytecode.ByteCodeSkeleton.ClassFile
import miksilo.modularLanguages.deltas.bytecode.constants.{ClassInfoConstant, Utf8ConstantDelta}
import miksilo.modularLanguages.deltas.bytecode.coreInstructions.InstructionInstance.Instruction
import miksilo.modularLanguages.deltas.bytecode.coreInstructions.InstructionSignature
import miksilo.modularLanguages.deltas.bytecode.simpleBytecode.InstructionTypeAnalysis.InstructionSideEffects
import miksilo.modularLanguages.deltas.bytecode.types.QualifiedObjectTypeDelta
import miksilo.modularLanguages.deltas.bytecode.{ByteCodeMethodInfo, ByteCodeSkeleton}

class InstructionTypeAnalysisForMethod(program: Node, language: Language, method: MethodInfo[Node]) {
  private val typeAnalysis = getTypeAnalysis
  val parameters: Seq[Node] = getMethodParameters
  private val initialVariables = parameters.zipWithIndex.map(p => p._2 -> p._1).toMap
  val initialStack: Seq[Node] = Seq[Node]()
  val initialProgramTypeState: ProgramTypeState = ProgramTypeState(initialStack, initialVariables)
  val typeStatePerInstruction: Map[Int, ProgramTypeState] = typeAnalysis.run(0, initialProgramTypeState)

  private def getTypeAnalysis = {
    val codeAnnotation = method.codeAttribute
    val instructions = codeAnnotation.instructions

    new InstructionTypeAnalysis(instructions) {
      override def getSideEffects(typeState: ProgramTypeState, instruction: Instruction[Node]): InstructionSideEffects =
        instruction.delta.getVariableUpdates(instruction, typeState)

      override def getSignature(typeState: ProgramTypeState, instruction: Instruction[Node]): InstructionSignature =
        instruction.delta.getSignature(instruction, typeState, language)
    }
  }

  private def getMethodParameters = {
    val methodIsStatic: Boolean = method.accessFlags.contains(ByteCodeMethodInfo.StaticAccess)
    val methodParameters = method._type.parameterTypes
    if (methodIsStatic) {
      methodParameters
    }
    else {
      val classFile: ClassFile[Node] = program
      val classRef = classFile(ByteCodeSkeleton.ClassNameIndexKey).asInstanceOf[Node]
      val className = classRef(ClassInfoConstant.Name).asInstanceOf[Node]
      Seq(QualifiedObjectTypeDelta.neww(Utf8ConstantDelta.toQualifiedClassName(className))) ++ methodParameters
    }
  }
}
