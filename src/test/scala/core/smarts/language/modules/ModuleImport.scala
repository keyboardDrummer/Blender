package core.smarts.language.modules

import core.smarts.ConstraintBuilder
import core.smarts.scopes.imports.ScopeImport
import core.smarts.scopes.objects.Scope

class ModuleImport(name: String) extends FakeSourceElement {

  def constraints(builder: ConstraintBuilder, scope: Scope): Unit = {
    val importedDeclaration = builder.declarationVariable()
    val importedScope = builder.resolveScopeDeclaration(importedDeclaration)
    builder.reference(name, this, scope, importedDeclaration)
    builder.add(ScopeImport(scope, importedScope))
  }
}