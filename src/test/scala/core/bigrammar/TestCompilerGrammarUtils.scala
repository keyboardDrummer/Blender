package core.bigrammar

import core.bigrammar.TestGrammarUtils.parseAndPrintSame
import core.bigrammar.printer.BiGrammarToPrinter
import core.grammar.{Grammar, GrammarToParserConverter}
import core.particles._
import core.particles.grammars.{GrammarCatalogue, ProgramGrammar}
import core.particles.node.GrammarKey
import org.scalatest.FunSuite
import transformations.javac.JavaCompilerDeltas
import util.{CompilerBuilder, TestUtils}

import scala.util.parsing.input.CharArrayReader

object TestCompilerGrammarUtils extends TestCompilerGrammarUtils(JavaCompilerDeltas.javaCompilerTransformations)

object TestGrammarUtils extends FunSuite {

  def parseAndPrintSame(example: String, expectedOption: Option[Any] = None, grammarDocument: BiGrammar): Unit = {
    val documentResult: String = parseAndPrint(example, expectedOption, grammarDocument)
    assertResult(example)(documentResult)
  }

  def parseAndPrint(example: String, expectedOption: Option[Any], grammarDocument: BiGrammar): String = {
    val parseResult = parse(example, grammarDocument)
    assert(parseResult.successful, parseResult.toString)

    val result = parseResult.get

    expectedOption.foreach(expected => assertResult(expected)(result))

    print(result, grammarDocument)
  }

  def print(result: Any, grammarDocument: BiGrammar): String = {
    BiGrammarToPrinter.toDocument(result, grammarDocument).renderString()
  }

  def parse(example: String, grammarDocument: BiGrammar) = {
    val grammar: Grammar = BiGrammarToGrammar.toGrammar(grammarDocument)

    val packratParser = GrammarToParserConverter.convert(grammar)
    val parseResult = packratParser(new CharArrayReader(example.toCharArray))
    parseResult
  }
}

case class TestCompilerGrammarUtils(particles: Seq[Delta]) extends FunSuite {

  def compareInputWithPrint(input: String, expected: Option[Any] = None, grammarTransformer: GrammarKey = ProgramGrammar) {
    parseAndPrintSame(input, expected, getGrammarUsingTransformer(grammarTransformer))
  }

  def getPrintResult(value: Any, grammarTransformer: GrammarKey = ProgramGrammar): String = {
    val document = getGrammarUsingTransformer(grammarTransformer)
    BiGrammarToPrinter.toDocument(value, document).renderString()
  }

  def getGrammarUsingTransformer(grammarTransformer: GrammarKey = ProgramGrammar): Labelled = {
    CompilerBuilder.build(getTransformations(grammarTransformer)).getGrammar
  }

  def getGrammarResult(input: String, grammarTransformer: GrammarKey = ProgramGrammar): Any = {
    val compiler = CompilerBuilder.build(getTransformations(grammarTransformer))
    compiler.parse(TestUtils.stringToInputStream(input))
  }

  def getTransformations(key: GrammarKey): Seq[Delta] = {
    Seq(new SelectorTransformation(key)) ++ particles
  }

  class SelectorTransformation(key: GrammarKey) extends DeltaWithGrammar {
    override def transformGrammars(grammars: GrammarCatalogue, state: Language): Unit = {
      if (key != ProgramGrammar)
        grammars.find(ProgramGrammar).inner = grammars.find(key)
    }

    override def dependencies: Set[Contract] = Set.empty

    override def description: String = "Sets the program grammar to a specific grammar from the grammar catalogue."
  }

}
