package transformations.bytecode.types

import core.bigrammar.printer.{BiGrammarToPrinter, BiGrammarToPrinter$}
import core.particles.exceptions.BadInputException
import core.grammar.ParseException
import core.particles._
import core.particles.grammars.GrammarCatalogue
import core.particles.node.Node
import transformations.bytecode.ByteCodeSkeleton

class TypeMismatchException(to: Node, from: Node) extends BadInputException {
  override def toString = s"cannot assign: $to = $from"
}

class NoCommonSuperTypeException(first: Node, second: Node) extends BadInputException

class AmbiguousCommonSuperTypeException(first: Node, second: Node) extends BadInputException

object TypeSkeleton extends DeltaWithGrammar with WithLanguageRegistry {
  def getTypeFromByteCodeString(state: Language, typeString: String): Node = {
    val manager = new DeltasToParserConverter()
    manager.parse(state.grammarCatalogue.find(ByteCodeTypeGrammar), typeString).asInstanceOf[Node]
  }

  def toStackType(_type: Node, state: Language) : Node = {
    getRegistry(state).instances(_type.clazz).getStackType(_type, state)
  }

  def getTypeSize(_type: Node, state: Language): Int = getRegistry(state).stackSize(_type.clazz)

  def getByteCodeString(state: Language)(_type: Node): String = {
      val grammar = state.grammarCatalogue.find(TypeSkeleton.ByteCodeTypeGrammar)
      BiGrammarToPrinter.toDocument(_type, grammar).renderString()
  }

  override def dependencies: Set[Contract] = Set(ByteCodeSkeleton)

  def checkAssignableTo(state: Language)(to: Node, from: Node) = {
    if (!isAssignableTo(state)(to, from))
      throw new TypeMismatchException(to, from)
  }

  def isAssignableTo(state: Language)(to: Node, from: Node): Boolean = {
    val fromSuperTypes = getSuperTypes(state)(from)
    if (to.equals(from))
      return true

    fromSuperTypes.exists(_type => _type.equals(to))
  }

  def getSuperTypes(state: Language)(_type: Node) = getSuperTypesRegistry(state)(_type.clazz)(_type)

  def getSuperTypesRegistry(state: Language) = {
    getRegistry(state).superTypes
  }

  def union(state: Language)(first: Node, second: Node): Node = {
    val filteredDepths = getAllSuperTypes(state)(first).map(depthTypes => depthTypes.filter(_type => isAssignableTo(state)(_type, second)))
    val resultDepth = filteredDepths.find(depth => depth.nonEmpty).getOrElse(throw new NoCommonSuperTypeException(first, second))
    if (resultDepth.size > 1)
      throw new AmbiguousCommonSuperTypeException(first, second)

    resultDepth.head
  }

  def getAllSuperTypes(state: Language)(_type: Node): Stream[Set[Node]] = {
    var returnedTypes = Set.empty[Node]
    Stream.iterate(Set(_type))(previousDepthTypes => {
      val result = previousDepthTypes.flatMap(_type => getSuperTypes(state)(_type)).diff(returnedTypes)
      returnedTypes ++= result
      result
    })
  }

  object ByteCodeTypeGrammar
  override def transformGrammars(grammars: GrammarCatalogue, state: Language): Unit = {
    grammars.create(JavaTypeGrammar)
    grammars.create(ByteCodeTypeGrammar)
  }

  def createRegistry = new Registry
  
  class Registry {
    val superTypes = new ClassRegistry[Node => Seq[Node]]()
    val stackSize = new ClassRegistry[Int]()
    val instances = new ClassRegistry[TypeInstance]
  }

  object JavaTypeGrammar

  override def description: String = "Defines the concept of a type."
}
