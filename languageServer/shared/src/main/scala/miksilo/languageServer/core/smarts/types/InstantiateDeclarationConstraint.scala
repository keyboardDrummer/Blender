package miksilo.languageServer.core.smarts.types

import miksilo.languageServer.core.smarts.ConstraintSolver
import miksilo.languageServer.core.smarts.objects.{Declaration, DeclarationVariable, NamedDeclaration}
import miksilo.languageServer.core.smarts.scopes.{DeclaresDeclaration, DeclaresScope}
import miksilo.languageServer.core.smarts.scopes.objects.{Scope, ScopeVariable}
import miksilo.languageServer.core.smarts.types.objects.{Type, TypeVariable}

case class InstantiateDeclarationConstraint(var _type: Type, var instantiated: Declaration, var template: Declaration) extends TypeConstraint {
  override def apply(solver: ConstraintSolver): Boolean = template match {
    case named:NamedDeclaration =>
      val scopeGraph = solver.scopeGraph
      val declaredScope = scopeGraph.nodes(named).collect({ case x: DeclaresScope => x}).head.target
      val fieldDeclarations = scopeGraph.nodes(declaredScope).collect({ case x: DeclaresDeclaration => x})

      def copy(d: NamedDeclaration): NamedDeclaration = new NamedDeclaration(d.name, d.origin)

      val declarationCopy = copy(named)

      val freeVariables: Set[TypeVariable] = fieldDeclarations.flatMap(d => solver.environment(d.target).variables).toSet
      if (freeVariables.size != 1)
        return false

      val typeParameter = freeVariables.head
      val declaredScopeCopy = solver.builder.newScope()
      scopeGraph.addEdge(declarationCopy, DeclaresScope(declaredScopeCopy))
      fieldDeclarations.foreach(d => {
        val originalDeclaration: NamedDeclaration = d.target
        val fieldDeclarationCopy: NamedDeclaration = copy(originalDeclaration)
        scopeGraph.addEdge(declaredScopeCopy, DeclaresDeclaration(fieldDeclarationCopy))
        solver.declare(fieldDeclarationCopy, solver.environment(originalDeclaration).instantiateType(typeParameter, _type))
      })

      val result = solver.unifyDeclarations(instantiated, declarationCopy)
      result
    case _ => false
  }

  override def instantiateDeclaration(variable: DeclarationVariable, instance: Declaration): Unit = {
    if (variable == instantiated)
      instantiated = instance
    if (variable == template)
      template = instance
  }

  override def instantiateType(variable: TypeVariable, instance: Type): Unit =  {
    if (variable == _type)
      _type = instance
  }

  override def instantiateScope(variable: ScopeVariable, instance: Scope): Unit = {}

  override def boundTypes: Set[Type] = Set.empty
}
