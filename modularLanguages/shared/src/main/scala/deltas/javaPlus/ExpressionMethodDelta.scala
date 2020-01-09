package deltas.javaPlus

import core.deltas._
import core.deltas.grammars.LanguageGrammars
import core.language.node.{Node, NodeField, NodeShape}
import core.language.{Compilation, Language}
import deltas.expression.ExpressionDelta
import deltas.javac.classes.skeleton.JavaClassDelta
import deltas.javac.classes.skeleton.JavaClassDelta.JavaClass
import deltas.javac.methods.MethodDelta.ReturnType
import deltas.javac.methods.{AccessibilityFieldsDelta, MethodDelta, ReturnExpressionDelta}
import deltas.statement.BlockDelta

object ExpressionMethodDelta extends DeltaWithGrammar with DeltaWithPhase {

  override def dependencies: Set[Contract] = Set(ReturnExpressionDelta, MethodDelta, JavaClassDelta)

  import deltas.HasNameDelta.Name

  object Shape extends NodeShape
  object Expression extends NodeField

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val visibilityGrammar = find(AccessibilityFieldsDelta.VisibilityField)
    val parseStatic = find(AccessibilityFieldsDelta.Static)
    val parseReturnType = find(MethodDelta.ReturnTypeGrammar).as(ReturnType)
    val parseParameters = find(MethodDelta.Parameters).as(MethodDelta.Parameters)
    val expressionGrammar = find(ExpressionDelta.FirstPrecedenceGrammar).as(Expression)
    val expressionMethodGrammar = (visibilityGrammar ~~ parseStatic ~~ parseReturnType ~~
      identifier.as(Name) ~ parseParameters ~~ ("=" ~~> expressionGrammar)).
      asNode(Shape)
    val methodGrammar = find(MethodDelta.Shape)
    methodGrammar.addAlternative(expressionMethodGrammar)
  }

  override def transformProgram(program: Node, state: Compilation): Unit = {
    val clazz: JavaClass[Node] = program
    for(expressionMethod <- clazz.members.filter(method => method.shape == Shape))
    {
      val expression = expressionMethod(Expression).asInstanceOf[Node]
      expressionMethod.shape = MethodDelta.Shape
      expressionMethod(MethodDelta.Body) = BlockDelta.neww(Seq(ReturnExpressionDelta.neww(expression)))
      expressionMethod.data.remove(Expression)
    }
  }

  override def description: String = "Allows method bodies to be defined using only an expression."
}
