package deltas.javac.types

import core.deltas.grammars.LanguageGrammars
import core.language.node.{Node, NodeLike, NodeShape}
import core.deltas.{Contract, DeltaWithGrammar, HasShape}
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import core.smarts.types.objects.Type
import deltas.bytecode.types.HasTypeDelta

object WildcardTypeArgument extends DeltaWithGrammar with HasTypeDelta with HasShape {

  override def description: String = "Adds the wildcard type argument '*'."

  object Shape extends NodeShape
  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val byteCodeArgumentGrammar = find(TypeApplicationDelta.ByteCodeTypeArgumentGrammar)
    byteCodeArgumentGrammar.addAlternative("*" ~> value(new Node(Shape)))

    val javaArgumentGrammar = find(TypeApplicationDelta.JavaTypeArgumentGrammar)
    javaArgumentGrammar.addAlternative("?" ~> value(new Node(Shape)))
  }

  override def getType(compilation: Compilation, builder: ConstraintBuilder, path: NodeLike, parentScope: Scope): Type = {
    core.smarts.types.objects.TypeVariable("?") //TODO not sure what to do here.
  }

  override def shape: NodeShape = Shape

  override def dependencies: Set[Contract] = Set(TypeApplicationDelta)
}
