package transformations.javac

import org.junit.{Assert, Test}
import transformations.javac.{FibonacciWthoutMain, TestUtils}
import transformations.bytecode._
import core.transformation.MetaObject
import transformations.javac.base.model._
import JavaClassModel._
import transformations.javac.base.model.QualifiedClassName
import JavaMethodModel._
import JavaTypes._
import JavaBaseModel._
import scala.collection.mutable.ArrayBuffer

class FibonacciWithMain {
  val className = "Fibonacci"
  val defaultPackage = Seq()
  val other = new FibonacciWthoutMain()

  @Test
  def runCompiledFibonacci() {
    val fibonacci = getJavaFibonacciWithMain
    val compiler = JavaCompiler.getCompiler
    val byteCode = compiler.compile(fibonacci)

    val expectedResult = 8
    TestUtils.runByteCode(className, byteCode, expectedResult)
  }

  def getMainMethodJava: MetaObject = {
    val parameters = Seq(parameter("args", arrayType(objectType(new QualifiedClassName(Seq("java", "lang", "String"))))))
    val fibCall = call(variable("fibonacci"), Seq(LiteralC.literal(5)))
    val body = Seq(call(selector(selector(selector(selector(variable("java"), "lang"), "System"), "out"), "print"), Seq(fibCall)))
    method("main", VoidType, parameters, body, static = true, PublicVisibility)
  }

  @Test
  def testStackSizeLocalsArgs() {
    val fibonacci = getJavaFibonacciWithMain
    val compiler = JavaCompiler.getCompiler
    val byteCode = compiler.compile(fibonacci)
    val methods = ByteCode.getMethods(byteCode)
    val constructor = methods(0)
    val main = methods(1)
    val fibonacciMethod = methods(2)
    Assert.assertEquals(1, getMethodMaxStack(constructor))
    Assert.assertEquals(2, getMethodMaxStack(main))
    Assert.assertEquals(3, getMethodMaxStack(fibonacciMethod))
    Assert.assertEquals(1, getMethodLocals(constructor))
    Assert.assertEquals(1, getMethodLocals(main))
    Assert.assertEquals(1, getMethodLocals(fibonacciMethod))

  }

  def getMethodLocals(method: MetaObject) = ByteCode.getCodeMaxLocals(ByteCode.getMethodAttributes(method)(0))
  def getMethodMaxStack(method: MetaObject) = ByteCode.getCodeMaxStack(ByteCode.getMethodAttributes(method)(0))

  @Test
  def compileAndPrintFibonacciWithMain() {
    val fibonacci = getJavaFibonacciWithMain
    val compiler = JavaCompiler.getCompiler
    val compiledCode = compiler.compile(fibonacci)
    PrintByteCode.print(compiledCode)
  }

  def getJavaFibonacciWithMain: MetaObject = {
    clazz(defaultPackage, className, Seq(getMainMethodJava, other.getFibonacciMethodJava))
  }

  @Test
  def compileAndValidateFibonacciWithMain() {
    val fibonacciJava = getJavaFibonacciWithMain
    val compiler = JavaCompiler.getCompiler
    val compiledCode = compiler.compile(fibonacciJava)
    val expectedCode = getExpectedUnoptimizedFibonacciWithMainByteCode
    TestUtils.testInstructionEquivalence(expectedCode, compiledCode)
    TestUtils.compareConstantPools(expectedCode, compiledCode)
  }

  val methodName = "fibonacci"

  def getExpectedUnoptimizedFibonacciWithMainByteCode: MetaObject = {
    val constantPool: ArrayBuffer[Any] = getConstantPool
    val method: MetaObject = getFibonacciMethodByteCode
    val nativeClass = ByteCode.clazz(3, 4, constantPool, Seq(other.getConstructorByteCode, getMainByteCode, method))
    nativeClass
  }

  def getMainByteCode = {
    val instructions = Seq(ByteCode.getStatic(2),
      ByteCode.integerConstant(5),
      ByteCode.invokeStatic(3),
      ByteCode.invokeVirtual(4),
      ByteCode.voidReturn)
    ByteCode.methodInfo(11, 12, Seq(ByteCode.codeAttribute(9, 2, 1, instructions, Seq(), Seq())),
      Set(ByteCode.PublicAccess, ByteCode.StaticAccess))
  }

  def getFibonacciMethodByteCode: MetaObject = {
    val instructions = Seq(
      ByteCode.integerLoad(0),
      ByteCode.integerConstant(2),
      ByteCode.ifIntegerCompareGreater(3),
      ByteCode.integerConstant(1),
      ByteCode.goTo(16),
      ByteCode.integerConstant(0),
      ByteCode.ifZero(7),
      ByteCode.integerConstant(1),
      ByteCode.goTo(16),
      ByteCode.integerLoad(0),
      ByteCode.integerConstant(1),
      ByteCode.subtractInteger,
      ByteCode.invokeStatic(3),
      ByteCode.integerLoad(0),
      ByteCode.integerConstant(2),
      ByteCode.subtractInteger,
      ByteCode.invokeStatic(3),
      ByteCode.addInteger,
      ByteCode.integerReturn
    )
    val method = ByteCode.methodInfo(13, 14, Seq(ByteCode.codeAttribute(9, 3, 1, instructions, Seq(), Seq())),
      Set(ByteCode.PublicAccess, ByteCode.StaticAccess))
    method
  }

  def getConstantPool: ArrayBuffer[Any] = {
    val constantPool = ArrayBuffer[Any](ByteCode.methodRef(6, 18),
      ByteCode.fieldRef(19, 20),
      ByteCode.methodRef(5, 21),
      ByteCode.methodRef(22, 23),
      ByteCode.classRef(24),
      ByteCode.classRef(25),
      ConstructorC.constructorName,
      ByteCode.methodDescriptor(JavaTypes.VoidType, Seq()),
      ByteCode.CodeAttributeId,
      "main",
      ByteCode.methodDescriptor(JavaTypes.VoidType, Seq(
        JavaTypes.arrayType(JavaTypes.objectType(new QualifiedClassName(Seq("java", "lang", "String")))))),
      methodName,
      ByteCode.methodDescriptor(JavaTypes.IntType, Seq(JavaTypes.IntType)),
      ByteCode.StackMapTableId,
      ByteCode.nameAndType(7, 8),
      ByteCode.classRef(26),
      ByteCode.nameAndType(27, 28),
      ByteCode.nameAndType(13, 14),
      ByteCode.classRef(29),
      ByteCode.nameAndType(30, 31),
      new QualifiedClassName(defaultPackage ++ Seq(className)),
      new QualifiedClassName(Seq("java", "lang", "Object")),
      new QualifiedClassName(Seq("java", "lang", "System")),
      "out",
      JavaTypes.objectType(new QualifiedClassName(Seq("java", "io", "PrintStream"))),
      new QualifiedClassName(Seq("java", "io", "PrintStream")),
      "print",
      ByteCode.methodDescriptor(JavaTypes.VoidType, Seq(JavaTypes.IntType))
    )
    constantPool
  }
}