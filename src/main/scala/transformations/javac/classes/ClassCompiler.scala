package transformations.javac.classes

import java.util
import java.util.NoSuchElementException

import core.particles.CompilationState
import core.particles.node.Node
import transformations.bytecode.ByteCodeSkeleton.ByteCode
import transformations.bytecode.constants.{FieldRefConstant, MethodRefConstant, NameAndType}
import transformations.bytecode.types.ObjectTypeC
import transformations.javac.classes.skeleton.JavaClassSkeleton._
import transformations.javac.classes.skeleton._

case class FieldInfo(parent: ClassSignature, name: String, _static: Boolean, _type: Node) extends ClassMember

case class MethodInfo(_type: Node, _static: Boolean) extends ClassMember

case class MethodQuery(className: QualifiedClassName, methodName: String, argumentTypes: Seq[Node])

case class ClassCompiler(currentClass: Node, compiler: JavaCompilerState) {
  val state: CompilationState = compiler.state
  val className: String = currentClass.name
  val myPackage: PackageSignature = compiler.getPackage(currentClass._package.toList)
  val currentClassInfo = ClassSignature(myPackage, className)
  myPackage.content(className) = currentClassInfo

  getState(state).classCompiler = this

  for (member <- getState(state).members)
    member.bind(state, currentClassInfo, currentClass)

  currentClass.constantPool = new ConstantPool()

  def constantPool: ConstantPool = currentClass.constantPool

  lazy val classNames: Map[String, QualifiedClassName] = getClassMapFromImports(currentClass.imports)

  def findClass(className: String): ClassSignature = compiler.find(fullyQualify(className).parts).asInstanceOf[ClassSignature]

  def fullyQualify(className: String): QualifiedClassName = {
    try
      {
        classNames(className)
      }
    catch {
      case (_:NoSuchElementException) =>
        throw new NoSuchElementException(s"Could not find $className in $classNames")
    }
  }

  def getMethodRefIndex(methodKey: MethodQuery): Int = {
    val classRefIndex = constantPool.getClassRef(methodKey.className)
    val nameAndTypeIndex = getMethodNameAndTypeIndex(methodKey)
    constantPool.store(MethodRefConstant.methodRef(classRefIndex, nameAndTypeIndex))
  }

  def getMethodNameAndTypeIndex(methodKey: MethodQuery): Int = {
    val methodNameIndex = getNameIndex(methodKey.methodName)
    val descriptorIndex = constantPool.store(compiler.find(methodKey)._type)
    val result: Node = NameAndType.nameAndType(methodNameIndex, descriptorIndex)
    constantPool.store(result)
  }

  def getNameIndex(methodName: String): Int = {
    constantPool.storeUtf8(methodName)
  }

  def getFieldRefIndex(info: FieldInfo): Int = {
    val classRef = getClassRef(info.parent)
    val fieldNameAndTypeIndex = getFieldNameAndTypeIndex(info)
    constantPool.store(FieldRefConstant.fieldRef(classRef, fieldNameAndTypeIndex))
  }

  def getClassRef(info: ClassSignature): Int = {
    constantPool.getClassRef(info.getQualifiedName)
  }

  def getFieldNameAndTypeIndex(info: FieldInfo): Int = {
    val fieldNameIndex = constantPool.storeUtf8(info.name)
    val typeIndex =   constantPool.store(info._type)
    val result: Node = NameAndType.nameAndType(fieldNameIndex, typeIndex)
    constantPool.store(result)
  }

  def findClass(objectType: Node): ClassSignature = {
    val qualifiedName = ObjectTypeC.getObjectTypeName(objectType) match {
      case Right(qualified) => qualified
      case Left(name) => fullyQualify(className)
    }
    compiler.find(qualifiedName.parts).asInstanceOf[ClassSignature]
  }

  private def getClassMapFromImports(imports: Seq[Node]): Map[String, QualifiedClassName] = {
    imports.flatMap(_import => {
      JavaClassSkeleton.getState(state).importToClassMap(_import.clazz)(_import)
    }).toMap ++ Map(className -> JavaClassSkeleton.getQualifiedClassName(currentClass))
  }
}