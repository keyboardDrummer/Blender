package transformations.bytecode.simpleBytecode

import core.particles.Compilation
import core.particles.node.Node
import transformations.bytecode.ByteCodeSkeleton._
import transformations.bytecode.attributes.CodeAttribute
import transformations.bytecode.attributes.CodeAttribute.JumpBehavior
import transformations.bytecode.constants.ClassInfoConstant
import transformations.bytecode.coreInstructions.InstructionSignature
import transformations.bytecode.extraConstants.{QualifiedClassNameConstant, TypeConstant}
import transformations.bytecode.simpleBytecode.InstructionTypeAnalysis.InstructionSideEffects
import transformations.bytecode.types.ObjectTypeC
import transformations.bytecode.{ByteCodeMethodInfo, ByteCodeSkeleton}
import transformations.javac.classes.skeleton.QualifiedClassName
import transformations.javac.types.MethodType._

class InstructionTypeAnalysisFromState(state: Compilation, method: Node) {
  val constantPool = state.program.constantPool

  val typeAnalysis = getTypeAnalysis
  val parameters = getMethodParameters
  val initialVariables = parameters.zipWithIndex.map(p => p._2 -> p._1).toMap
  val initialStack = Seq[Node]()
  val initialProgramTypeState: ProgramTypeState = ProgramTypeState(initialStack, initialVariables)
  val typeStatePerInstruction = typeAnalysis.run(0, initialProgramTypeState)

  private def getTypeAnalysis = {
    val codeAnnotation = ByteCodeMethodInfo.getMethodAttributes(method).find(a => a.clazz == CodeAttribute.CodeKey).get
    val instructions = CodeAttribute.getCodeInstructions(codeAnnotation)

    new InstructionTypeAnalysis(instructions) {
      val instructionVariableUpdateRegistry = CodeAttribute.getState(state).localUpdates
      override def getSideEffects(typeState: ProgramTypeState, instruction: Node): InstructionSideEffects =
        instructionVariableUpdateRegistry(instruction.clazz).getVariableUpdates(instruction, typeState)

      val instructionSignatureRegistry = CodeAttribute.getInstructionSignatureRegistry(state)
      override def getSignature(typeState: ProgramTypeState, instruction: Node): InstructionSignature =
        instructionSignatureRegistry(instruction.clazz).getSignature(instruction, typeState, state)

      val jumpBehaviorRegistry = CodeAttribute.getState(state).jumpBehaviorRegistry
      override def getJumpBehavior(instructionClazz: Any): JumpBehavior = jumpBehaviorRegistry(instructionClazz)
    }
  }
  
  private def getMethodParameters = {
    val methodIsStatic: Boolean = ByteCodeMethodInfo.getMethodAccessFlags(method).contains(ByteCodeMethodInfo.StaticAccess)
    val methodType = TypeConstant.getValue(constantPool.getValue(ByteCodeMethodInfo.getMethodDescriptorIndex(method)).asInstanceOf[Node])
    val methodParameters = methodType.parameterTypes
    if (methodIsStatic) {
      methodParameters
    }
    else {
      val clazz = state.program
      val clazzRefIndex = clazz(ByteCodeSkeleton.ClassNameIndexKey).asInstanceOf[Int]
      val clazzRef = constantPool.getValue(clazzRefIndex).asInstanceOf[Node]
      val className = constantPool.getValue(ClassInfoConstant.getNameIndex(clazzRef)).asInstanceOf[Node]
      Seq(ObjectTypeC.objectType(className(QualifiedClassNameConstant.Value).asInstanceOf[QualifiedClassName])) ++ methodParameters
    }
  }
}
