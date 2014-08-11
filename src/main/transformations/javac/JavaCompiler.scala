package transformations.javac

import core.transformation._
import core.transformation.sillyCodePieces.Injector
import transformations.bytecode.coreInstructions._
import transformations.bytecode.coreInstructions.integerCompare.{IfIntegerCompareGreaterOrEqualC, IfIntegerCompareLessC, IfZeroC}
import transformations.bytecode.extraBooleanInstructions.{LessThanInstructionC, OptimizeBooleanInstructionsC}
import transformations.bytecode.simpleBytecode.{InferredMaxStack, InferredStackFrames}
import transformations.bytecode.{ByteCodeSkeleton, LabelledTargets}
import transformations.javac.base.JavaMethodAndClassC
import transformations.javac.expressions._
import transformations.javac.methods._
import transformations.javac.statements._
import transformations.javac.types._

object JavaCompiler {
  val typeTransformations = Seq(ObjectTypeC, ArrayTypeC, BooleanTypeC, LongTypeC, VoidTypeC, IntTypeC, TypeC)

  def getCompiler = new CompilerFromTransformations(javaCompilerTransformations)

  def javaCompilerTransformations: Seq[Injector] = {
    Seq(ImplicitThisInPrivateCalls, ImplicitJavaLangImport, DefaultConstructor, ImplicitSuperConstructorCall,
      ImplicitObjectSuperClass, ImplicitReturnAtEndOfMethod, ConstructorC, LessThanC, TernaryC, EqualityC,
      AddEqualityPrecedence, AddRelationalPrecedence, AdditionC, SubtractionC, AddAdditivePrecedence, LiteralC,
      AssignmentC, CallC, ReturnC, SelectorC, VariableC, ParenthesisC, NullC, DeclarationC, JavaMethodAndClassC, WhileC, BlockC,
      StatementC, ExpressionC) ++ allByteCodeTransformations
  }

  def allByteCodeTransformations = Seq(OptimizeBooleanInstructionsC) ++ Seq(LessThanInstructionC) ++ simpleByteCodeTransformations

  def simpleByteCodeTransformations = Seq(InferredStackFrames, InferredMaxStack, LabelledTargets) ++ byteCodeTransformations

  def byteCodeTransformations = typeTransformations ++ byteCodeInstructions ++ Seq(ByteCodeSkeleton)

  def byteCodeInstructions: Seq[InstructionC] = {
    Seq(AddIntegersC, GetStaticC, GotoC, IfIntegerCompareLessC, IfIntegerCompareGreaterOrEqualC,
      IfZeroC, IncrementIntegerC, IntegerConstantC, IntegerReturnC, InvokeSpecialC, InvokeVirtualC, InvokeStaticC,
      LoadAddressC, LoadIntegerC, PushNullC, StoreAddressC, StoreIntegerC, SubtractIntegerC, VoidReturnC)
  }

  def getTransformer = new Transformer(javaCompilerTransformations)
}



