package miksilo.modularLanguages.util

import miksilo.modularLanguages.core.deltas._
import miksilo.languageServer.core.language.Compilation
import miksilo.modularLanguages.core.node.Node
import miksilo.modularLanguages.deltas.bytecode.{ByteCodeLanguage, PrintByteCode}
import miksilo.editorParser.SourceUtils
import miksilo.languageServer.util.StreamUtils

import scala.reflect.io.{File, Path}
import scala.sys.process.{Process, ProcessLogger}

object JavaSourceUtils {

  def getJavaTestFile(fileName: String, inputDirectory: Path = Path("")): String = {
    val className = JavaSourceUtils.fileNameToClassName(fileName)
    val relativeFilePath = inputDirectory / (className + ".java")
    StreamUtils.streamToString(SourceUtils.getResourceFile(relativeFilePath))
  }

  class LineProcessLogger extends ProcessLogger {
    var line = ""

    /** Will be called with each line read from the process output stream.
      */
    def out(s: => String): Unit = {
      if (!s.contains("Picked up _JAVA_OPTIONS"))
        line += s
    }

    /** Will be called with each line read from the process error stream.
      */
    def err(s: => String): Unit = {
      if (!s.contains("Picked up _JAVA_OPTIONS"))
        line += s
    }

    def buffer[T](f: => T): T = f
  }

  def runJavaClass(className: String, directory: Path): String = {
    val processBuilder = Process.apply(s"java $className", directory.jfile)
    val logger = new LineProcessLogger()
    processBuilder ! logger
    logger.line
  }

  def getBytes(byteCode: Node): LazyList[Byte] = {
    var output: LazyList[Byte] = null
    val deltas: Seq[Delta] = Seq(new GetBytes(s => output = s)) ++ ByteCodeLanguage.byteCodeDeltas
    TestLanguageBuilder.build(deltas).compileAst(byteCode)
    output
  }

  class GetBytes(write: LazyList[Byte] => Unit) extends DeltaWithPhase {
    override def transformProgram(program: Node, compilation: Compilation): Unit = {
      write(PrintByteCode.getBytes(compilation, program))
    }

    override def description: String = "Writes the current program as JVM class file bytes to a function."

    override def dependencies: Set[Contract] = Set.empty
  }

  def runByteCode(className: String, code: Node) : String = {
    val bytes = getBytes(code).toArray
    val currentDir = new File(new java.io.File("."))
    val testDirectory = currentDir / Path("testOutput")
    testDirectory.createDirectory()
    val byteCodeFile = File.apply(testDirectory / Path(className).addExtension("class"))
    val writer = byteCodeFile.outputStream(append = false)
    writer.write(bytes)
    writer.close()

    runJavaClass(className, testDirectory)
  }

  def fileNameToClassName(fileName: Path): String = {
    val name = fileName.name
    if (name.endsWith(".java")) name.dropRight(5) else name
  }

  def getJavaTestFileContents(fileName: String, inputDirectory: Path = Path("")): String = {
    val className = fileNameToClassName(fileName)
    val relativeFilePath = inputDirectory / (className + ".java")
    SourceUtils.getResourceFileContents(relativeFilePath)
  }
}