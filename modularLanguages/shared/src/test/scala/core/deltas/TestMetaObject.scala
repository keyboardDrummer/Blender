package core.deltas

import core.language.node.{Node, NodeField, NodeShape}
import deltas.expression.{IntLiteralDelta, VariableDelta}
import deltas.javac.methods.MemberSelectorDelta
import deltas.javac.methods.call.CallDelta
import org.scalatest.funsuite.AnyFunSuite

class TestMetaObject extends AnyFunSuite {

  test("Equals") {
    val first = new Node(ShapeKey, FieldKey -> FieldValue)
    val second = new Node(ShapeKey, FieldKey -> FieldValue)
    assertResult(first)(second)
  }

  test("EqualsOnJavaModel") {
    val first = CallDelta.neww(MemberSelectorDelta.neww(MemberSelectorDelta.neww(VariableDelta.neww("System"), "out"), "print"),
      List(CallDelta.neww(VariableDelta.neww("fibonacci"), List(IntLiteralDelta.neww(5)))))
    val second = CallDelta.neww(MemberSelectorDelta.neww(MemberSelectorDelta.neww(VariableDelta.neww("System"), "out"), "print"),
      List(CallDelta.neww(VariableDelta.neww("fibonacci"), List(IntLiteralDelta.neww(5)))))
    assertResult(first)(second)
  }

  object ShapeKey extends NodeShape

  object FieldKey extends NodeField

  object FieldValue extends NodeField
}
