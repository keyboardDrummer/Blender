package deltas.json

import core.bigrammar.grammars.{Keyword, Parse, RegexGrammar}
import core.deltas.Delta
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.NodePath
import core.language.exceptions.BadInputException
import core.language.node._
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import core.smarts.types.objects.Type
import deltas.expressions.ExpressionDelta
import deltas.javac.expressions.ExpressionInstance

case class DuplicateObjectLiteralKeys(duplicates: Seq[String]) extends BadInputException

object JsonObjectLiteralDelta extends ExpressionInstance with Delta {

  override def description: String = "Adds the JSON object literal to expressions"

  override def transformGrammars(grammars: LanguageGrammars, language: Language): Unit = {
    import grammars._

    val expressionGrammar = find(ExpressionDelta.FirstPrecedenceGrammar)
    val keyGrammar = "\"" ~> RegexGrammar(JsonStringLiteralDelta.stringInnerRegex).as(MemberKey) ~< "\""
    val member = keyGrammar ~ ":" ~~ expressionGrammar.as(MemberValue) asNode MemberShape
    val inner = "{" % (member.manySeparatedVertical(",").as(Members) ~ Parse(Keyword(",") | value(Unit))).indent() % "}"
    val grammar = inner.asLabelledNode(Shape)
    expressionGrammar.addAlternative(grammar)
  }

  object MemberValue extends NodeField
  object MemberKey extends NodeField
  object MemberShape extends NodeShape

  object Members extends NodeField
  object Shape extends NodeShape
  override def shape: NodeShape = Shape

  override def constraints(compilation: Compilation, builder: ConstraintBuilder, expression: NodePath, _type: Type, parentScope: Scope): Unit = {

  }

  override def getType(expression: NodePath, compilation: Compilation): Node = ???

  implicit class ObjectLiteralMember[T <: NodeLike](val node: T) extends NodeWrapper[T] {
    def key: String = node(MemberKey).asInstanceOf[String]
    def value: T = node(MemberValue).asInstanceOf[T]
  }

  implicit class ObjectLiteral[T <: NodeLike](val node: T) extends NodeWrapper[T] {
    def getValue(key: String): T = members.find(member => member.key == key).get.value
    def members: Seq[ObjectLiteralMember[T]] = NodeWrapper.wrapList(node(Members).asInstanceOf[Seq[T]])
  }
}
