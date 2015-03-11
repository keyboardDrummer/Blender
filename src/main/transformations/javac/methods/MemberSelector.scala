package transformations.javac.methods

import core.particles.grammars.GrammarCatalogue
import core.particles.{WithState, CompilationState, MetaObject, ParticleWithGrammar}
import transformations.javac.classes.{ClassInfo, ReferenceKind, ClassOrObjectReference, ClassCompiler}
import transformations.javac.expressions.ExpressionSkeleton

object MemberSelector extends ParticleWithGrammar with WithState {

  def getSelectorObject(selector: MetaObject) = selector(SelectorObject).asInstanceOf[MetaObject]

  def getSelectorMember(selector: MetaObject) = selector(SelectorMember).asInstanceOf[String]

  override def transformGrammars(grammars: GrammarCatalogue): Unit = {
    val core = grammars.find(ExpressionSkeleton.CoreGrammar)
    val expression = grammars.find(ExpressionSkeleton.ExpressionGrammar)
    val selection = (expression <~ ".") ~ identifier ^^ parseMap(SelectorKey, SelectorObject, SelectorMember)
    core.addOption(grammars.create(SelectGrammar, selection))
  }

  object SelectGrammar

  object SelectorKey

  object SelectorObject

  object SelectorMember

  def selector(_object: Any, member: Any): MetaObject = selector(_object.asInstanceOf[MetaObject], member.asInstanceOf[String])

  def selector(_object: MetaObject, member: String): MetaObject = {
    new MetaObject(SelectorKey) {
      data.put(SelectorObject, _object)
      data.put(SelectorMember, member)
    }
  }


  def getClassOrObjectReference(selector: MetaObject, compiler: ClassCompiler): ClassOrObjectReference = {
    val obj = getSelectorObject(selector)
    getReferenceKind(compiler, obj).asInstanceOf[ClassOrObjectReference]
  }

  def getReferenceKind(classCompiler: ClassCompiler, expression: MetaObject): ReferenceKind = {
    val getReferenceKindOption = MemberSelector.getReferenceKindRegistry(classCompiler.state).get(expression.clazz)
    getReferenceKindOption.fold[ReferenceKind]({
      getReferenceKindFromExpressionType(classCompiler, expression)
    })(implementation => implementation(expression))
  }

  def getReferenceKindFromExpressionType(classCompiler: ClassCompiler, expression: MetaObject): ClassOrObjectReference = {
    val classInfo: ClassInfo = classCompiler.findClass(ExpressionSkeleton.getType(classCompiler.state)(expression))
    new ClassOrObjectReference(classInfo, false)
  }

  def getReferenceKindRegistry(state: CompilationState) = getState(state).referenceKindRegistry
  class State {
    val referenceKindRegistry = new ClassRegistry[MetaObject => ReferenceKind]()
  }

  override def createState = new State()

  override def description: String = "Defines the selector grammar <expression>.<identifier>"
}
