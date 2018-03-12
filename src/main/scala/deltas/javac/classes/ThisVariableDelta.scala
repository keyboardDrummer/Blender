package deltas.javac.classes

import core.bigrammar.BiGrammar
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.NodePath
import core.deltas.{Contract, DeltaWithGrammar}
import core.language.node.GrammarKey
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import core.smarts.types.objects.TypeFromDeclaration
import deltas.javac.classes.skeleton.{HasConstraints, JavaClassSkeleton}
import deltas.javac.methods.VariableDelta
import deltas.javac.methods.VariableDelta.{Name, Shape}

object ThisVariableDelta extends DeltaWithGrammar
{
  object Grammar extends GrammarKey

  val thisName = "this"

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val variable = find(VariableDelta.VariableGrammar)
    val thisGrammar = create(Grammar, (thisName: BiGrammar).as(Name)).asNode(Shape)
    variable.addAlternative(thisGrammar)
  }

  override def inject(language: Language): Unit = {
    JavaClassSkeleton.hasConstraints.add(language, JavaClassSkeleton.Shape, new HasConstraints {
      override def collectConstraints(compilation: Compilation, builder: ConstraintBuilder, path: NodePath, parentScope: Scope): Unit = {
        val classScope = JavaClassSkeleton.getClassScope(compilation, builder, path, parentScope)
        val clazz: JavaClassSkeleton.JavaClass[NodePath] = path
        val clazzName = clazz.name
        val classDeclaration = builder.resolve(clazzName, path, classScope)
        builder.declare(thisName, classScope, _type = Some(TypeFromDeclaration(classDeclaration)))
      }
    })
    super.inject(language)
  }

  override def dependencies: Set[Contract] = Set(VariableDelta)

  override def description: String = "Enables using the '" + thisName + "' qualifier to refer to the current instance."
}