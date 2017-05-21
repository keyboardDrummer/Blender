package transformations.javac

import core.particles.CompilerFromParticles
import core.particles.node.Node
import transformations.bytecode.readJar.ClassFileSignatureDecompiler
import transformations.javac.classes.ClassCompiler
import transformations.javac.classes.skeleton.{MyCompiler, PackageSignature}
import util.TestUtils

object JavaLang {

  val compiler = new CompilerFromParticles(ClassFileSignatureDecompiler.getDecompiler)
  val systemClass: Node = compiler.parseAndTransform(TestUtils.getTestFile("System.class")).program
  val printStreamClass: Node = compiler.parseAndTransform(TestUtils.getTestFile("PrintStream.class")).program
  val objectClass: Node = compiler.parseAndTransform(TestUtils.getTestFile("Object.class")).program
  val stringClass: Node = compiler.parseAndTransform(TestUtils.getTestFile("String.class")).program

  def initialise(compiler: MyCompiler) {
    new ClassCompiler(objectClass, compiler)
    new ClassCompiler(stringClass, compiler)
    new ClassCompiler(systemClass, compiler)
    new ClassCompiler(printStreamClass, compiler)
  }

  val classPath = new PackageSignature(None, "")
}
