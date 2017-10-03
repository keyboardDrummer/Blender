package application.compilerCockpit

import java.io.InputStream

import core.bigrammar.printer.{BiGrammarToPrinter, PrintError, BiGrammarToPrinter$}
import core.bigrammar.{BiGrammarToGrammar, BiGrammar}
import core.particles.grammars.ProgramGrammar
import core.particles.{Phase, Language, CompilerFromDeltas, Delta}
import core.responsiveDocument.ResponsiveDocument

import scala.util.Try

case class PrettyPrint(recover: Boolean = false) extends Delta
{
  override def inject(state: Language): Unit = {
    val foundGrammar = state.grammarCatalogue.find(ProgramGrammar)
    state.data(this) = foundGrammar.deepClone

    state.compilerPhases = List(Phase(this.name, this.description, compilation => {
      val grammar = getOutputGrammar(state)
      val documentTry: Try[ResponsiveDocument] = Try(BiGrammarToPrinter.toDocument(compilation.program, grammar))
      val documentTryWithOptionalRecover: Try[ResponsiveDocument] = if (recover) {
        documentTry.recover({ case e: PrintError => e.toDocument })
      }
      else {
        documentTry
      }
      val document: ResponsiveDocument = documentTryWithOptionalRecover.get
      compilation.output = document.renderString()
    }))
  }

  override def description: String = "Prints the program by generating a pretty printer from its grammar."

  def getOutputGrammar(state: Language) = state.data(this).asInstanceOf[BiGrammar]
}

object PrettyPrintOption extends CompileOption {

  override def perform(cockpit: CompilerCockpit, input: InputStream): TextWithGrammar = {
    val prettyPrint = PrettyPrint(recover = true)
    val splicedParticles = cockpit.compiler.replace(MarkOutputGrammar,Seq(prettyPrint))
    val compiler = new CompilerFromDeltas(splicedParticles)

    val state = compiler.parseAndTransform(input)
    val outputGrammar = prettyPrint.getOutputGrammar(state.language)
    TextWithGrammar(state.output, BiGrammarToGrammar.toGrammar(outputGrammar))
  }

  override def toString = "Pretty Print"
}
