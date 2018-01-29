package deltas.bytecode.types

import core.bigrammar.BiGrammar
import core.bigrammar.grammars.{Keyword, Labelled}
import core.deltas.Compilation
import core.deltas.grammars.LanguageGrammars
import core.deltas.node.{GrammarKey, Node, NodeField, NodeShape}
import core.deltas.path.NodePath
import core.language.Language
import core.nabl.ConstraintBuilder
import core.nabl.scopes.objects.Scope
import core.nabl.types.objects.{TypeFromDeclaration, Type}
import deltas.bytecode.constants.ClassInfoConstant
import deltas.bytecode.extraConstants.QualifiedClassNameConstantDelta
import deltas.javac.classes.skeleton.QualifiedClassName
import util.Utility

object ObjectTypeDelta extends TypeInstance with StackType {
  override val key = ObjectTypeKey
  val stringType = objectType(QualifiedClassName(Seq("java", "lang", "String")))
  val rootObjectType = objectType(QualifiedClassName(Seq("java", "lang", "Object")))

  override def getSuperTypes(_type: Node, state: Language): Seq[Node] = {
    Seq.empty //TODO extend
  }

  def stackObjectType(constantPoolClassRef: Int) = new Node(ObjectTypeKey, Name -> constantPoolClassRef)

  object ObjectTypeJavaGrammar extends GrammarKey
  override def getJavaGrammar(grammars: LanguageGrammars): BiGrammar = {
    import grammars._
    val construct: Seq[String] => Either[String, QualifiedClassName] = stringIds => {
      if (stringIds.size > 1)
        Right(QualifiedClassName(stringIds))
      else
        Left(stringIds.last)
    }
    def deconstruct(value: Either[String, QualifiedClassName]): Seq[String] = value match {
      case Right(QualifiedClassName(stringIds)) => stringIds
      case Left(string) => Seq(string)
    }
    val parseObjectType = create(ObjectTypeJavaGrammar,
      identifier.someSeparated(".").map[Seq[String], Either[String, QualifiedClassName]](construct, deconstruct).
        as(Name).asNode(ObjectTypeKey))
    parseObjectType
  }

  def objectType(name: QualifiedClassName) = new Node(ObjectTypeKey,
    Name -> Right(name))

  def objectType(className: String) = new Node(ObjectTypeKey,
    Name -> Left(className))

  object ObjectTypeByteCodeGrammar extends GrammarKey
  object ObjectTypeByteCodeGrammarInner extends GrammarKey
  override def getByteCodeGrammar(grammars: LanguageGrammars): BiGrammar = {
    val construct: QualifiedClassName => Either[String, QualifiedClassName] = Right[String, QualifiedClassName]
    def deconstruct(value: Any): Option[Any] = Some(value match {
      case Right(name) => name
    })

    val qualifiedClassNameParser = QualifiedClassNameConstantDelta.getQualifiedClassNameParser(grammars)
    import grammars._
    val inner: Labelled = create(ObjectTypeByteCodeGrammarInner,
      qualifiedClassNameParser.mapSome[QualifiedClassName, Either[String, QualifiedClassName]](
        Right[String, QualifiedClassName],
        v => Utility.cast[Right[String, QualifiedClassName]](v).map(v => v.value)).as(Name).asNode(ObjectTypeKey))
    val grammar: BiGrammar = Keyword("L", reserved = false) ~> inner ~< ";"
    create(ObjectTypeByteCodeGrammar, grammar)
  }

  def getObjectTypeName(objectType: Node): Either[String, QualifiedClassName] = objectType(Name).asInstanceOf[Either[String, QualifiedClassName]]

  override def getStackSize: Int = 1

  object Name  extends NodeField

  object ObjectTypeKey extends NodeShape

  object ObjectStackType extends NodeShape

  override def getStackType(_type: Node, state: Language): Node = {
    ObjectStackType.create(Name -> ClassInfoConstant.classRef(ObjectTypeDelta.getObjectTypeName(_type).right.get))
  }

  override def description: String = "Defines the object type."

  override def getType(compilation: Compilation, builder: ConstraintBuilder, _type: NodePath, parentScope: Scope): Type = {
    val classDeclaration = builder.resolve(name, _type, parentScope)
    TypeFromDeclaration(classDeclaration)
  }
}
