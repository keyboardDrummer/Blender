package miksilo.modularLanguages.core.node

import miksilo.modularLanguages.core.deltas.path.{AnyPath, ChildPath, NodePath}

import scala.collection.mutable

class TypedChildField[T](name: String) extends NodeField {
  def get(path: AnyPath): Option[T] = getFieldData(path).get(this).asInstanceOf[Option[T]]
  def apply(path: AnyPath): T = getFieldData(path)(this).asInstanceOf[T]

  private def getFieldData(path: AnyPath): mutable.HashMap[NodeField, Any] = {
    path match {
      case nodePath: NodePath => nodePath.data
      case childPath: ChildPath => childPath.parent.current.fieldData.getOrElseUpdate(childPath.field, new mutable.HashMap)
    }
  }

  def update(path: AnyPath, value: T): Unit = getFieldData(path)(this) = value

  override lazy val toString = name
}
