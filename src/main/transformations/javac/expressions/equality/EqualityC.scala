package transformations.javac.expressions.equality

import core.grammar._
import core.transformation._
import core.transformation.grammars.GrammarCatalogue
import core.transformation.sillyCodePieces.GrammarTransformation
import transformations.javac.expressions.ExpressionC

object EqualityC extends GrammarTransformation {
  override def dependencies: Set[Contract] = Set(AddEqualityPrecedence)

  override def inject(state: TransformationState): Unit = {
    ExpressionC.getExpressionToLines(state).put(EqualityC, equality => {
      val first = getFirst(equality)
      val second = getSecond(equality)
      val toInstructions = ExpressionC.getToInstructions(state)
      toInstructions(first) ++ toInstructions(second) ++ ???
    })
  }

  def getFirst(equality: MetaObject) = equality(FirstKey).asInstanceOf[MetaObject]

  def getSecond(equality: MetaObject) = equality(SecondKey).asInstanceOf[MetaObject]

  override def transformGrammars(grammars: GrammarCatalogue): Unit = {
    val equalityGrammar = grammars.find(AddEqualityPrecedence.EqualityExpressionGrammar)
    val parseEquality = (equalityGrammar <~ "==") ~ equalityGrammar ^^ { case left seqr right => equality(left.asInstanceOf[MetaObject], right.asInstanceOf[MetaObject])}
    equalityGrammar.inner = equalityGrammar.inner | equalityGrammar
  }

  def equality(first: MetaObject, second: MetaObject) = new MetaObject(EqualityKey, FirstKey -> first, SecondKey -> second)

  object EqualityKey

  object FirstKey

  object SecondKey

}