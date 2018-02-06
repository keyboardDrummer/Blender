package deltas.javac.types

import core.deltas.grammars.LanguageGrammars
import core.deltas.node.{Node, NodeLike, NodeShape}
import core.deltas.{Compilation, DeltaWithGrammar, HasShape}
import core.language.Language
import core.nabl.ConstraintBuilder
import core.nabl.scopes.objects.Scope
import core.nabl.types.objects.Type
import deltas.bytecode.types.HasType

object WildcardTypeArgument extends DeltaWithGrammar with HasType with HasShape {

  override def description: String = "Adds the wildcard type argument '*'."

  object Shape extends NodeShape
  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val byteCodeArgumentGrammar = find(TypeApplicationDelta.ByteCodeTypeArgumentGrammar)
    byteCodeArgumentGrammar.addOption("*" ~> value(new Node(Shape)))

    val javaArgumentGrammar = find(TypeApplicationDelta.JavaTypeArgumentGrammar)
    javaArgumentGrammar.addOption("?" ~> value(new Node(Shape)))
  }

  override def getType(compilation: Compilation, builder: ConstraintBuilder, path: NodeLike, parentScope: Scope): Type = {
    core.nabl.types.objects.TypeVariable("?") //TODO not sure what to do here.
  }

  override def shape: NodeShape = Shape
}

