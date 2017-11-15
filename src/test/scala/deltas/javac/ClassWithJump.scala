package deltas.javac

import org.scalatest.FunSuite
import util.TestUtils

import scala.reflect.io.Path

class ClassWithJump extends FunSuite {

  test("basic") {
    val inputDirectory = Path("")
    TestUtils.compareWithJavacAfterRunning("ClassWithJump", inputDirectory)
  }
}
