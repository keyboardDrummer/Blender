package core.bigrammar

object UndefinedExpression extends TestExpression {
  override def compute = ???
  override def toString = "Undefined"
}

case class Value(value: Int) extends TestExpression {
  override def compute = value
}

trait TestExpression {
  def compute: Int
}

case class Add(first: TestExpression, second: TestExpression) extends TestExpression {
  override def compute = first.compute + second.compute
}

case class Multiply(first: TestExpression, second: TestExpression) extends TestExpression {
  override def compute = first.compute * second.compute
}

case class IfNotZero(condition: TestExpression, _then: TestExpression, _else: TestExpression) extends TestExpression {
  override def compute = if (condition.compute == 0) _then.compute else _else.compute
}

