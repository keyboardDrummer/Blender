package core.smarts.scopes

import core.language.SourceElement
import core.smarts.objects.{NamedDeclaration, Reference}
import core.smarts.scopes.objects.ConcreteScope

import scala.collection.mutable

trait GraphNode
case class ScopeNode(scope: ConcreteScope) extends GraphNode
{
  override def toString: String = scope.toString
}
case class ReferenceNode(reference: Reference) extends GraphNode
{
  override def toString: String = reference.toString
}
case class DeclarationNode(declaration: NamedDeclaration) extends GraphNode
{
  override def toString: String = declaration.toString
}


trait GraphEdge {
  def target: GraphNode
  def traverse: Boolean
}
case class ReferenceEdge(target: ScopeNode) extends GraphEdge
{
  override def traverse: Boolean = true
}
case class ImportEdge(target: ScopeNode) extends GraphEdge {
  override def traverse: Boolean = true
}
case class DeclaresDeclaration(target: DeclarationNode) extends GraphEdge {
  override def traverse: Boolean = true
}
case class Parent(target: ScopeNode) extends GraphEdge {
  override def traverse: Boolean = true
}
case class DeclaresScope(target: ScopeNode) extends GraphEdge {
  override def traverse: Boolean = false
}

/*
References are inside scopes.
Scopes can be inside other scopes.
Declarations are inside scopes.
A declaration can declare a scope.
 */
class ScopeGraph extends scala.collection.mutable.HashMap[GraphNode, mutable.Set[GraphEdge]]
{
  def declarations: Seq[DeclarationNode] = this.keys.collect({case x: DeclarationNode => x}).toSeq
  def findReference(location: SourceElement): Option[Reference] = this.keys.collect({ case n: ReferenceNode => n.reference }).
    find(n => n.origin.map(x => x.position).contains(location.position))
  def resolveLocation(location: SourceElement): SourceElement = resolve(findReference(location).get).origin

  def addImport(currentScope: ConcreteScope, importedScope: ConcreteScope): Unit = add(ScopeNode(currentScope), ImportEdge(ScopeNode(importedScope)))

  def resolveScope(importedModule: NamedDeclaration): ConcreteScope = {
    val reachableNodes = depthFirst(DeclarationNode(importedModule)).collect({case d:ScopeNode => d})
    if (reachableNodes.nonEmpty)
    {
      return reachableNodes.head.scope
    }
    null
  }

  def addReference(reference: Reference, currentScope: ConcreteScope): Unit = add(ReferenceNode(reference), ReferenceEdge(ScopeNode(currentScope)))

  def resolve(reference: Reference): NamedDeclaration = {
    val reachableNodes = depthFirst(ReferenceNode(reference)).collect({case d:DeclarationNode => d}).
      filter(d => d.declaration.name == reference.name)
    if (reachableNodes.nonEmpty)
    {
      return reachableNodes.head.declaration
    }
    null
  }

  case class DebugNode(node: GraphNode, graph: ScopeGraph) {
    def next: Seq[DebugNode] = {
      graph(node).map(n => DebugNode(n.target, graph)).toSeq
    }
  }

  def debug(node: GraphNode): DebugNode = new DebugNode(node, this)

  def depthFirst(root: GraphNode): Seq[GraphNode] = {
    var result = List.empty[GraphNode]
    val visited = mutable.Set.empty[GraphNode]
    val queue = new mutable.Queue[GraphNode]
    queue.enqueue(root)
    while(queue.nonEmpty)
    {
      val element = queue.dequeue()
      if (visited.add(element))
      {
        result ::= element
        this.get(element).foreach(x => x.filter(c => c.traverse).foreach(c => queue.enqueue(c.target)))
      }
    }
    result.reverse
  }

  def parent(child: ConcreteScope, parent: ConcreteScope): Unit = add(ScopeNode(child), Parent(ScopeNode(parent)))
  def declareDeclaration(inside: ConcreteScope, declaration: NamedDeclaration): Unit = add(ScopeNode(inside), DeclaresDeclaration(DeclarationNode(declaration)))
  def declareScope(declaration: NamedDeclaration, scope: ConcreteScope): Unit = add(DeclarationNode(declaration), DeclaresScope(ScopeNode(scope)))

  def add(node: GraphNode, edge: GraphEdge): Boolean =
  {
    val edges = this.getOrElseUpdate(node, mutable.Set.empty)
    edges.add(edge)
  }
}