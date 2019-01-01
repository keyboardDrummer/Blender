package core.bigrammar

import core.deltas.Delta
import core.smarts.SolveConstraintsDelta
import deltas.expression.ExpressionDelta
import deltas.javac.constructor.{ConstructorDelta, DefaultConstructorDelta, ImplicitSuperConstructorCall}
import deltas.javac.methods.{ImplicitReturnAtEndOfMethod, MethodDelta}
import deltas.javac.{ImplicitJavaLangImport, ImplicitObjectSuperClass, ImplicitThisForPrivateMemberSelectionDelta, JavaLanguage}
import deltas.statement.BlockDelta
import deltas.{PrettyPrint, RunWithJVM}
import org.scalatest.FunSuite
import util.{SourceUtils, TestLanguageBuilder}

import scala.reflect.io.Path

class TestDocumentGrammarWithJavaExamples extends FunSuite {
  val lineSeparator: String = System.lineSeparator()

  test("SimpleForLoop") {
    val testFileContent = SourceUtils.getJavaTestFileContents("SimpleForLoop", Path(""))
    TestLanguageGrammarUtils.compareInputWithPrint(testFileContent, None)
  }

  test("While") {
    val testFileContent = SourceUtils.getJavaTestFileContents("Whilee", Path(""))
    TestLanguageGrammarUtils.compareInputWithPrint(testFileContent, None)
  }

  test("Fibonacci") {
    val testFileContent = SourceUtils.getJavaTestFileContents("Fibonacci", Path(""))
    TestLanguageGrammarUtils.compareInputWithPrint(testFileContent, None)
  }

  test("Ternary") {
    val input = "1 ? 2 : 3"
    TestLanguageGrammarUtils.compareInputWithPrint(input, None, ExpressionDelta.FirstPrecedenceGrammar)
  }

  test("SystemPrintX") {
    val input = s"System.print(x)"
    TestLanguageGrammarUtils.compareInputWithPrint(input, None, ExpressionDelta.FirstPrecedenceGrammar)
  }

  /*
  Deze case is lastig omdat Selector.Member eerst print is, maar daarna out wordt,
  en daarna verdwijnt voordat print gehandled wordt.

  Hoe werkt dit tijdens parsen?
   */
  test("SystemOutPrintX") {
    val input = s"System.out.print(x)"
    TestLanguageGrammarUtils.compareInputWithPrint(input, None, ExpressionDelta.FirstPrecedenceGrammar)
  }

  test("FibonacciMainMethod") {
    val input = s"public static void main(java.lang.String[] args)$lineSeparator{$lineSeparator    System.out.print(fibonacci(5));$lineSeparator}"
    TestLanguageGrammarUtils.compareInputWithPrint(input, None, MethodDelta.MethodGrammar)
  }

  test("Block") {
    val input = "{" + lineSeparator + "    System.out.print(fibonacci(5));" + lineSeparator + "}"
    TestLanguageGrammarUtils.compareInputWithPrint(input, None, BlockDelta.Grammar)
  }

  test("PrintAfterImplicitAddition") {
    val input = SourceUtils.getJavaTestFile("Fibonacci", Path(""))
    val expectation = SourceUtils.getJavaTestFileContents("ExplicitFibonacci.java")

    val implicits = Seq[Delta](ImplicitJavaLangImport, DefaultConstructorDelta, ImplicitSuperConstructorCall,
      ImplicitObjectSuperClass, ConstructorDelta, ImplicitReturnAtEndOfMethod, SolveConstraintsDelta, ImplicitThisForPrivateMemberSelectionDelta)
    val newTransformations = TestLanguageBuilder.buildWithParser(JavaLanguage.javaCompilerDeltas).spliceAfterTransformations(implicits, Seq(new PrettyPrint))

    val state = TestLanguageBuilder.buildWithParser(newTransformations).compile(input)
    val output = state.output

    assertResult(expectation)(output)
  }

  test("PrettyPrintByteCode") {
    val input = SourceUtils.getJavaTestFile("Fibonacci", Path(""))
    val expectation = SourceUtils.getTestFileContents("FibonacciByteCodePrettyPrinted.txt")

    val prettyPrintCompiler = JavaLanguage.getPrettyPrintJavaToByteCodeCompiler

    val state = prettyPrintCompiler.compileStream(input)
    assertResult(expectation)(state.output)
  }

  test("PrettyPrintAndParseByteCode") {
    val input = SourceUtils.getJavaTestFile("Fibonacci.java", Path(""))

    val byteCodeTransformations = JavaLanguage.byteCodeDeltas
    val prettyPrintCompiler = JavaLanguage.getPrettyPrintJavaToByteCodeCompiler

    val state = prettyPrintCompiler.compileStream(input)
    val byteCode = state.output

    val parseTransformations = Seq(RunWithJVM) ++ byteCodeTransformations
    val output = TestLanguageBuilder.buildWithParser(parseTransformations).compile(SourceUtils.stringToStream(byteCode)).output
    assertResult("8")(output)
  }

  test("prettyPrintByteCode") {
    val input = SourceUtils.getTestFileContents("FibonacciByteCodePrettyPrinted.txt")
    val parseTransformations = Seq(new PrettyPrint) ++ JavaLanguage.byteCodeDeltas
    val output = TestLanguageBuilder.buildWithParser(parseTransformations).compile(SourceUtils.stringToStream(input)).output
    assertResult(input)(output)
  }

  test("parseByteCode") {
    val input = SourceUtils.getTestFileContents("FibonacciByteCodePrettyPrinted.txt")
    val deltas = Seq(RunWithJVM) ++ JavaLanguage.byteCodeDeltas
    val output = TestLanguageBuilder.buildWithParser(deltas).compile(SourceUtils.stringToStream(input)).output
    assertResult("8")(output)
  }
}
