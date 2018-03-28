package deltas.javac.types

import core.bigrammar.BiGrammar
import core.bigrammar.grammars.BiFailure
import core.deltas.grammars.LanguageGrammars
import core.language.node._
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import core.smarts.types.objects.{FunctionType, Type}
import deltas.bytecode.types.{ByteCodeTypeInstance, TypeSkeleton}

object MethodType extends ByteCodeTypeInstance {

  implicit class MethodTypeWrapper[T <: NodeLike](node: T) {
    def returnType: T = node(ReturnType).asInstanceOf[T]
    def returnType_=(value: T) = node(ReturnType) = value

    def parameterTypes: Seq[T] = node(Parameters).asInstanceOf[Seq[T]]
    def parameterTypes_=(value: Seq[T]) = node(Parameters) = value
  }

  def construct(returnType: Node, parameterTypes: Seq[Node]) = {
    new Node(MethodTypeKey,
      Parameters -> parameterTypes,
      ReturnType -> returnType,
      ThrowsSignature -> Seq.empty[Node])
  }

  object MethodTypeKey extends NodeShape

  object Parameters extends NodeField

  object ReturnType extends NodeField

  object ThrowsSignature extends NodeField

  val shape = MethodTypeKey

  override def description: String = "Defines the method type."

  override def getSuperTypes(_type: Node): Seq[Node] = ???

  override def getJavaGrammar(grammars: LanguageGrammars): BiGrammar = BiFailure()

  override def getByteCodeGrammar(grammars: LanguageGrammars): BiGrammar = {
    import grammars._
    val typeGrammar = find(TypeSkeleton.ByteCodeTypeGrammar)
    val throwsGrammar = ("^" ~> typeGrammar)*
    val methodGrammar = (("(" ~> (typeGrammar*).as(Parameters) ~< ")") ~ typeGrammar.as(ReturnType) ~ throwsGrammar.as(ThrowsSignature)).asNode(MethodTypeKey)
    methodGrammar
  }

  override def getType(compilation: Compilation, builder: ConstraintBuilder, _type: NodeLike, parentScope: Scope): Type = {
    val parameters = _type.parameterTypes
    val returnTypeNode = _type.returnType
    getType(compilation, builder, parentScope, parameters, returnTypeNode)
  }

  def getType(compilation: Compilation, builder: ConstraintBuilder, parentScope: Scope, parameters: Seq[NodeLike], returnTypeNode: NodeLike): Type = {
    val parameterTypes = parameters.map(parameter => TypeSkeleton.getType(compilation, builder, parameter, parentScope))
    val returnType = TypeSkeleton.getType(compilation, builder, returnTypeNode, parentScope)
    FunctionType.curry(parameterTypes, returnType)
  }
}