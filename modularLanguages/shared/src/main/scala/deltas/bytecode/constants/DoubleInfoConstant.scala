package deltas.bytecode.constants

import core.bigrammar.BiGrammar
import core.deltas.grammars.LanguageGrammars
import core.language.node.{Node, NodeField, NodeShape}
import core.language.{Compilation, Language}
import deltas.bytecode.PrintByteCode

object DoubleInfoConstant extends ConstantPoolEntry {

  implicit class LongConstantEntry(node: Node) {
    def value: Long = node(DoubleEntryValue).asInstanceOf[Long]
  }

  def construct(value: Double) = new Node(DoubleEntryKey, DoubleEntryValue -> value)

  object DoubleEntryKey extends NodeShape
  object DoubleEntryValue extends NodeField
  override def shape = DoubleEntryKey

  override def getBytes(compilation: Compilation, constant: Node): Seq[Byte] = PrintByteCode.byteToBytes(6) ++ PrintByteCode.longToBytes(constant.value)

  override def getConstantEntryGrammar(grammars: LanguageGrammars): BiGrammar = {
    import grammars._
    number.as(DoubleEntryValue)
  }

  override def description: String = "Add the double constant entry."

  override val getName = "Double"
}