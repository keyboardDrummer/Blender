package transformations.javac.methods.assignment

import core.biGrammar.BiFailure
import core.particles._
import core.particles.grammars.GrammarCatalogue
import core.particles.node.{MetaObject, MetaLike}
import core.particles.path.Path
import transformations.bytecode.coreInstructions.integers.StoreIntegerC
import transformations.bytecode.coreInstructions.objects.StoreAddressC
import transformations.bytecode.coreInstructions.{Duplicate2InstructionC, DuplicateInstructionC}
import transformations.javac.expressions.{ExpressionInstance, ExpressionSkeleton}
import transformations.javac.methods.MethodC
import transformations.types.TypeSkeleton

object AssignmentSkeleton extends ExpressionInstance with WithState {

  def getAssignmentTarget[T <: MetaLike](assignment: T) = assignment(AssignmentTarget).asInstanceOf[T]

  def getAssignmentValue[T <: MetaLike](assignment: T) = assignment(AssignmentValue).asInstanceOf[T]

  override def dependencies: Set[Contract] = Set(MethodC, StoreAddressC, StoreIntegerC, AssignmentPrecedence)

  override def transformGrammars(grammars: GrammarCatalogue): Unit = {
    val targetGrammar = grammars.create(AssignmentTargetGrammar, BiFailure)
    val expressionGrammar = grammars.find(ExpressionSkeleton.ExpressionGrammar)
    val assignmentGrammar = (targetGrammar <~~ "=") ~~ expressionGrammar ^^ parseMap(AssignmentKey, AssignmentTarget, AssignmentValue)
    expressionGrammar.addOption(assignmentGrammar)
  }

  object AssignmentTargetGrammar

  def assignment(target: MetaObject, value: MetaObject) = new MetaObject(AssignmentKey, AssignmentTarget -> target, AssignmentValue -> value)

  object AssignmentKey

  object AssignmentTarget

  object AssignmentValue

  override val key: AnyRef = AssignmentKey

  override def getType(assignment: Path, state: CompilationState): MetaObject = {
    val target = getAssignmentTarget(assignment)
    ExpressionSkeleton.getType(state)(target)
  }

  def createState = new State()
  class State {
    val assignFromStackByteCodeRegistry = new ClassRegistry[Path => Seq[MetaObject]]
  }

  override def toByteCode(assignment: Path, state: CompilationState): Seq[MetaObject] = {
    val value = getAssignmentValue(assignment)
    val valueInstructions = ExpressionSkeleton.getToInstructions(state)(value)
    val target = getAssignmentTarget(assignment)
    val assignInstructions = getState(state).assignFromStackByteCodeRegistry(target.clazz)(target)
    val valueType = ExpressionSkeleton.getType(state)(value)
    val duplicateInstruction = TypeSkeleton.getTypeSize(valueType, state) match
    {
      case 1 => DuplicateInstructionC.duplicate
      case 2 =>  Duplicate2InstructionC.duplicate
    }
    valueInstructions ++ Seq(duplicateInstruction) ++ assignInstructions
  }

  override def description: String = "Enables assignment to an abstract target using the = operator."
}
