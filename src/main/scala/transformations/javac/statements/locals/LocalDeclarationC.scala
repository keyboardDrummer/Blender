package transformations.javac.statements.locals

import core.particles._
import core.particles.exceptions.BadInputException
import core.particles.grammars.GrammarCatalogue
import core.particles.node.{Node, NodeClass, NodeField}
import core.particles.path.Path
import transformations.bytecode.types.TypeSkeleton
import transformations.javac.classes.skeleton.JavaClassSkeleton
import transformations.javac.statements.{StatementInstance, StatementSkeleton}

object LocalDeclarationC extends StatementInstance {

  def getDeclarationType(declaration: Node) = declaration(Type).asInstanceOf[Node]

  def getDeclarationName(declaration: Node) = declaration(Name).asInstanceOf[String]

  override def dependencies: Set[Contract] = Set(StatementSkeleton)

  override def transformGrammars(grammars: GrammarCatalogue, state: Language): Unit = {
    val statement = grammars.find(StatementSkeleton.StatementGrammar)
    val typeGrammar = grammars.find(TypeSkeleton.JavaTypeGrammar)
    val parseDeclaration = typeGrammar ~~ identifier ~< ";" asNode(DeclarationKey, Type, Name)
    statement.addOption(parseDeclaration)
  }

  def declaration(name: String, _type: Node): Node = {
    new Node(DeclarationKey, Name -> name, Type -> _type)
  }

  case class VariableAlreadyDefined(variable: String) extends BadInputException
  {
    override def toString = s"variable '$variable' was defined more than once."
  }

  object DeclarationKey extends NodeClass
  object Name extends NodeField
  object Type extends NodeField

  override val key = DeclarationKey

  override def toByteCode(declaration: Path, state: Language): Seq[Node] = {
    Seq.empty[Node]
  }

  override def definedVariables(state: Language, declaration: Node): Map[String, Node] = {
    val _type = getDeclarationType(declaration)
    JavaClassSkeleton.fullyQualify(_type, JavaClassSkeleton.getClassCompiler(state))
    val name: String = getDeclarationName(declaration)
    Map(name -> _type)
  }

  override def description: String = "Enables declaring a local variable."
}
