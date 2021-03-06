package miksilo.modularLanguages.deltas.bytecode.simpleBytecode

import miksilo.modularLanguages.core.bigrammar.BiGrammar
import miksilo.modularLanguages.core.deltas.grammars.LanguageGrammars
import miksilo.modularLanguages.core.node._
import miksilo.languageServer.core.language.{Compilation, Language}
import miksilo.modularLanguages.deltas.bytecode.attributes.StackMapTableAttributeDelta.StackMapFrameGrammar
import miksilo.modularLanguages.deltas.bytecode.coreInstructions.{InstructionInstance, InstructionSignature}

import scala.collection.mutable

object LabelDelta extends InstructionInstance {

  def Shape = shape //TODO inline

  object Name extends NodeField

  object StackFrame extends NodeField

  implicit class Label[T <: NodeLike](val node: T) extends NodeWrapper[T] {
    def stackFrame: Node = node(StackFrame).asInstanceOf[Node]
    def name: String = node(Name).asInstanceOf[String]
  }

  def label(name: String, stackFrame: Node): Node = Shape.create(
    Name -> name,
    StackFrame -> stackFrame)

  override def getBytes(compilation: Compilation, instruction: Node): Seq[Byte] = throw new UnsupportedOperationException()

  override def getSignature(instruction: Node, typeState: ProgramTypeState, language: Language): InstructionSignature = {
    InstructionSignature(Seq.empty, Seq.empty)
  }

  override def getInstructionSize(compilation: Compilation): Int = 0

  def getNameGrammar(grammars: LanguageGrammars): BiGrammar = grammars.regexGrammar("""[\w<>\-]+""".r, "label name")
  override def getGrammarForThisInstruction(_grammars: LanguageGrammars): BiGrammar = {
    val grammars = _grammars
    import grammars._
    val stackMapFrameGrammar = find(StackMapFrameGrammar)
    grammarName ~~> getNameGrammar(grammars).as(Name) %
      stackMapFrameGrammar.indent().as(StackFrame) asNode Shape
  }

  override def description: String = "Used to mark a specific point in an instruction list."

  override def grammarName: String = "label"

  object GeneratedLabels extends NodeField

  def getUniqueLabel(suggestion: String, methodInfo: Node): String = {
    val taken: mutable.Set[String] = methodInfo.data.getOrElseUpdate(GeneratedLabels, mutable.Set.empty).
      asInstanceOf[mutable.Set[String]]
    var result = suggestion
    var increment = 0
    while(taken.contains(result))
    {
      increment += 1
      result = suggestion + "_" + increment
    }
    taken.add(result)
    "<" + result + ">"
  }
}
