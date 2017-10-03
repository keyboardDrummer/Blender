package core.particles.node

trait Key extends AnyRef
{
  override lazy val toString: String = Node.classDebugRepresentation(this)

  /**
    * This hashcode does not change over runs, while the default hashcode does.
    * This makes the compilation process more deterministic.
    */
  override def hashCode(): Int = this.getClass.toString.hashCode
}

/**
  * Defines a field for a Node
  */
trait NodeField extends Key

/**
  * Defines a new Node class
  */
trait NodeClass extends Key {
  def create(values: (NodeField, Any)*): Node = {
    new Node(this, values: _*)
  }
}