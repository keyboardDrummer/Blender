package typed.languages.javaBytecode

import scala.collection.mutable
import util.TestConsole
import core.transformation.MetaObject

class Frame
{
  val operands: mutable.Stack[Any] = mutable.Stack()
  val variables: mutable.Seq[Any] = mutable.Seq()
}

class JavaByteCodeMachine(val console: TestConsole) {
  val stack: mutable.Stack[Frame] = mutable.Stack()
  def currentFrame = stack(0)
  def operands = currentFrame.operands
  def variables = currentFrame.variables

  def run(byteCode: ClassFile) = ???
}