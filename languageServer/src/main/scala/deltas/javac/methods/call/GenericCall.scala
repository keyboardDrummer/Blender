package deltas.javac.methods.call

import core.deltas.Contract
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.NodePath
import core.language.node._
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.objects.Reference
import core.smarts.scopes.ReferenceInScope
import core.smarts.scopes.objects.Scope
import core.smarts.types.objects.Type
import deltas.javac.classes.skeleton.JavaClassSkeleton
import deltas.javac.classes.{ClassCompiler, ClassOrObjectReference, MethodQuery}
import deltas.javac.expressions.{ExpressionInstance, ExpressionSkeleton, ToByteCodeSkeleton}
import deltas.javac.methods.call.CallDelta.Call
import deltas.javac.methods.{MemberSelectorDelta, NamespaceOrObjectExpression}
import deltas.javac.types.MethodType._

trait GenericCall extends ExpressionInstance {

  override def dependencies: Set[Contract] = Set(MemberSelectorDelta)

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val core = find(ExpressionSkeleton.CoreGrammar)
    val expression = find(ExpressionSkeleton.ExpressionGrammar)
    val selectorGrammar = find(MemberSelectorDelta.SelectGrammar)
    val calleeGrammar = create(CallDelta.Callee, selectorGrammar)
    val callArguments = create(CallDelta.CallArgumentsGrammar, "(" ~> expression.manySeparated(",") ~< ")")
    val parseCall = calleeGrammar.as(CallDelta.Callee) ~ callArguments.as(CallDelta.Arguments) asNode CallDelta.Shape
    core.addAlternative(parseCall)
  }

  override val shape = CallDelta.Shape

  override def getType(path: NodePath, compilation: Compilation): Node = {
    val call: Call[NodePath] = path
    val compiler = JavaClassSkeleton.getClassCompiler(compilation)
    val methodKey = getMethodKey(call, compiler)
    val methodInfo = compiler.javaCompiler.find(methodKey)
    val returnType = methodInfo._type.returnType
    returnType
  }

  def getGenericCallInstructions(call: Call[NodePath], compilation: Compilation, calleeInstructions: Seq[Node], invokeInstructions: Seq[Node]): Seq[Node] = {
    val expressionToInstruction = ToByteCodeSkeleton.getToInstructions(compilation)
    val callArguments = call.arguments
    val argumentInstructions = callArguments.flatMap(argument => expressionToInstruction(argument))
    calleeInstructions ++ argumentInstructions ++ invokeInstructions
  }

  def getMethodKey(call: Call[NodePath], compiler: ClassCompiler): MethodQuery = {
    val callCallee = call.callee
    val objectExpression = callCallee.target
    val kind = MemberSelectorDelta.getReferenceKind(compiler, objectExpression).asInstanceOf[ClassOrObjectReference]

    val callArguments = call.arguments
    val callTypes: Seq[Node] = callArguments.map(argument => ExpressionSkeleton.getType(compiler.compilation)(argument))

    val member = callCallee.member
    MethodQuery(kind.info.getQualifiedName, member, callTypes)
  }

  override def constraints(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, returnType: Type, parentScope: Scope): Unit = {
    val call: Call[NodePath] = path
    val callCallee = call.callee
    val calleeTarget = callCallee.target
    val calleeMember = callCallee.member

    val calleeReference = new Reference(calleeMember, Some(callCallee.getLocation(MemberSelectorDelta.Member)))
    val targetScope = NamespaceOrObjectExpression.getScope(compilation, builder, calleeTarget, parentScope)
    builder.add(ReferenceInScope(calleeReference, targetScope))
    CallDelta.callConstraints(compilation, builder, call.arguments, parentScope, calleeReference, returnType)
  }
}