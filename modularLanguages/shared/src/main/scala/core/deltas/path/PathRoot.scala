package core.deltas.path

import core.language.node.{Key, Node}
import miksilo.editorParser.parsers.{EndPointer, StartPointer}
import miksilo.editorParser.parsers.editorParsers.{OffsetPointerRange, OffsetRange, Position, SourceRange}

case class PathRoot(current: Node) extends NodePath with Key {

  override def parentOption: Option[NodePath] = None

  override def hashCode(): Int = 1 //TODO obj.hashCode

  override def equals(obj: Any): Boolean = obj.isInstanceOf[PathRoot] //TODO && obj.equals..
  override def pathAsString: String = "Root"

  override def rangeOption: Option[OffsetPointerRange] = Some(OffsetPointerRange(StartPointer, EndPointer))
}






