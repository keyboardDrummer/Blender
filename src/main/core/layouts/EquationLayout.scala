package core.layouts

import org.apache.commons.math3.linear.{LUDecomposition, MatrixUtils}

case class Equation(elements: Map[Variable, Double], constant: Double)


class EquationLayout() {
  private var components = Set.empty[Component]
  val container = createComponent
  var expressions = Set.empty[Expression]

  def createComponent = {
    val result = new Component(components.size)
    components += result
    result
  }

  def addEquals(expressions: Expression*) {
    for (pair <- expressions.zip(expressions.drop(1))) {
      this.expressions += pair._1 - pair._2
    }
  }

  def addRow(components: Component*) {
    addEquals(components.map(c => c.height).toSeq:_*)
    addEquals(components.map(c => c.bottom).toSeq:_*)
  }

  def addLeftToRight(components: Component*) {
    for (pair <- components.zip(components.drop(1))) {
      expressions += pair._1.right - pair._2.left
    }
  }

  def addTopToBottom(components: Component*) {
    for (pair <- components.zip(components.drop(1))) {
      expressions += pair._1.bottom - pair._2.top
    }
  }

  def solve(width: Int, height: Int): Map[Variable, Double] = {
    val expressionsWithContainer = expressions ++ Seq(container.left - width, container.right, container.bottom, container.top - height)
    val equations = expressionsWithContainer.map(e => e.toEquation).toSeq

    val expectedVariables = components.flatMap(component => Seq(component.bottom, component.top, component.left, component.right))
    val variables: Set[Variable] = equations.flatMap(equation => equation.elements.keySet).toSet

    val intersection = expectedVariables.intersect(variables)
    val union = expectedVariables.union(variables)
    val diff = union.diff(intersection)
    if (diff.nonEmpty)
      throw new RuntimeException(s"variable issue, diff = $diff")

    val orderedVariables = variables.toSeq
    def elementsToRow(elements: Map[Variable, Double]) = orderedVariables.map(variable => elements.getOrElse(variable, 0.0))
    val a = MatrixUtils.createRealMatrix(equations.map(equation => elementsToRow(equation.elements).toArray).toArray)
    val solver = new LUDecomposition(a).getSolver
    val b = MatrixUtils.createRealVector(equations.map(equation => -1 * equation.constant).toArray)
    val x = solver.solve(b)
    orderedVariables.zipWithIndex.map(indexedVariable => indexedVariable._1 -> x.getEntry(indexedVariable._2)).toMap
  }
}