package deltas.javaPlus

import org.scalatest.FunSuite
import deltas.javac.JavaToByteCodeLanguage
import util.TestLanguageBuilder
import util.LanguageTest

import scala.reflect.io.Path

class TestExpressionMethod extends FunSuite {

  test("basic") {
    val inputDirectory = Path("")
    val compiler = TestLanguageBuilder.buildWithParser(Seq(ExpressionMethodDelta) ++ JavaToByteCodeLanguage.javaCompilerDeltas)
    val result = new LanguageTest(compiler).compileAndRun("FibonacciWithExpressionMethod", inputDirectory)
    assertResult(8)(Integer.parseInt(result))
  }
}