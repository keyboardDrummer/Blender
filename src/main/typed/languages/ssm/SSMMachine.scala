package typed.languages.ssm

import scala.collection.mutable

class SSMMachine(program: SSMProgram) {
  val stack = mutable.Stack[Int]()
  val registers = Array.fill(10)(0)
  val labelLines = program.instructions.zipWithIndex
    .filter(p => p._1.isInstanceOf[Label])
    .map(p => (p._1.asInstanceOf[Label].name, p._2)).toMap

  def programCounter = registers(0)
  def programCounter_=(value: Int) {
    registers.update(0,value)
  }

  def popBoolean() = pop() != 0
  def pop() : Int = stack.pop()
  def push(value: Int) : Unit = stack.push(value)
  def push(value: Boolean) : Unit = push(if (value) 1 else 0)
  def goTo(label: String) = programCounter = labelLines(label)

  def run() {
    val instructions = program.instructions
    while(programCounter < instructions.length)
    {
      val instruction = instructions(programCounter)
      programCounter += 1
      instruction.execute(this)
    }
  }
}