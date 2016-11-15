package transformations.bytecode.attributes

import core.bigrammar.{BiGrammar, ManyVertical}
import core.particles._
import core.particles.grammars.GrammarCatalogue
import core.particles.node.{Key, Node}
import transformations.bytecode.PrintByteCode._
import transformations.bytecode.coreInstructions.InstructionSignature
import transformations.bytecode.readJar.ClassFileParser
import transformations.bytecode.simpleBytecode.ProgramTypeState
import transformations.bytecode.{ByteCodeMethodInfo, ByteCodeSkeleton}
import ClassFileParser._

object InstructionArgumentsKey extends Key

object CodeAttribute extends ByteCodeAttribute with WithState {

  def instruction(_type: AnyRef, arguments: Seq[Any] = Seq()) = new Node(_type, InstructionArgumentsKey -> arguments)

  def getInstructionArguments(instruction: Node) = instruction(InstructionArgumentsKey).asInstanceOf[Seq[Int]]

  def setInstructionArguments(instruction: Node, arguments: Seq[Any]) {
    instruction(InstructionArgumentsKey) = arguments
  }

  def getInstructionSizeRegistry(state: CompilationState) = getState(state).getInstructionSizeRegistry

  def getInstructionSignatureRegistry(state: CompilationState) = getState(state).getInstructionSignatureRegistry

  override def dependencies: Set[Contract] = Set(ByteCodeSkeleton, CodeConstantEntry)

  def codeAttribute(nameIndex: Integer, maxStack: Integer, maxLocals: Integer,
                    instructions: Seq[Node],
                    exceptionTable: Seq[Node],
                    attributes: Seq[Node]) = {
    new Node(CodeKey,
      ByteCodeSkeleton.AttributeNameKey -> nameIndex,
      CodeMaxStackKey -> maxStack,
      CodeMaxLocalsKey -> maxLocals,
      CodeInstructionsKey -> instructions,
      CodeExceptionTableKey -> exceptionTable,
      CodeAttributesKey -> attributes)
  }

  trait InstructionSignatureProvider
  {
    def getSignature(instruction: Node, programTypeState: ProgramTypeState, state: CompilationState): InstructionSignature
  }

  trait InstructionSideEffectProvider
  {
    def getVariableUpdates(instruction: Node, typeState: ProgramTypeState): Map[Int, Node]
  }

  case class JumpBehavior(movesToNext: Boolean, hasJumpInFirstArgument: Boolean)

  def createState = new State()
  class State {
    val getInstructionSignatureRegistry = new ClassRegistry[InstructionSignatureProvider]
    val getInstructionSizeRegistry = new ClassRegistry[Int]
    val jumpBehaviorRegistry = new ClassRegistry[JumpBehavior]
    val localUpdates = new ClassRegistry[InstructionSideEffectProvider]
  }

  override def inject(state: CompilationState): Unit = {
    super.inject(state)
    ByteCodeSkeleton.getState(state).getBytes(CodeKey) = attribute => getCodeAttributeBytes(attribute, state)
  }

  def getCodeAttributeBytes(attribute: Node, state: CompilationState): Seq[Byte] = {

    def getInstructionByteCode(instruction: Node): Seq[Byte] = {
      ByteCodeSkeleton.getState(state).getBytes(instruction.clazz)(instruction)
    }

    val exceptionTable = CodeAttribute.getCodeExceptionTable(attribute)
    shortToBytes(CodeAttribute.getCodeMaxStack(attribute)) ++
      shortToBytes(CodeAttribute.getCodeMaxLocals(attribute)) ++
      prefixWithIntLength(() => CodeAttribute.getCodeInstructions(attribute).flatMap(getInstructionByteCode)) ++
      shortToBytes(exceptionTable.length) ++
      exceptionTable.flatMap(exception => getExceptionByteCode(exception)) ++
      getAttributesByteCode(state, CodeAttribute.getCodeAttributes(attribute))
  }


  def getCodeAnnotations(clazz: Node): Seq[Node] = {
    ByteCodeSkeleton.getMethods(clazz)
      .flatMap(methodInfo => ByteCodeMethodInfo.getMethodAttributes(methodInfo))
      .flatMap(annotation => if (annotation.clazz == CodeKey) Some(annotation) else None)
  }

  def getCodeMaxStack(code: Node) = code(CodeMaxStackKey).asInstanceOf[Int]

  def getCodeMaxLocals(code: Node) = code(CodeMaxLocalsKey).asInstanceOf[Int]

  def getCodeExceptionTable(code: Node) = code(CodeExceptionTableKey).asInstanceOf[Seq[Node]]

  def getCodeAttributes(code: Node) = code(CodeAttributesKey).asInstanceOf[Seq[Node]]

  def getCodeInstructions(code: Node) = code(CodeInstructionsKey).asInstanceOf[Seq[Node]]


  object CodeKey extends Key

  object CodeMaxStackKey extends Key

  object CodeMaxLocalsKey extends Key

  object CodeInstructionsKey extends Key

  object CodeExceptionTableKey extends Key

  object CodeAttributesKey extends Key

  object InstructionGrammar

  object CodeGrammar

  override def key: Key = CodeKey

  object MaxStackGrammar
  override def getGrammar(grammars: GrammarCatalogue): BiGrammar = {
    val attributesGrammar = grammars.find(ByteCodeSkeleton.AttributesGrammar)
    val instructionGrammar: BiGrammar = grammars.create(InstructionGrammar)
    val maxStackGrammar = grammars.create(MaxStackGrammar, ("," ~~> "maxStack:" ~> integer).as(CodeMaxStackKey))
    val maxLocalGrammar = ("," ~~> "maxLocal:" ~> integer).as(CodeMaxLocalsKey)
    val header: BiGrammar = ("code: nameIndex:" ~> integer).as(ByteCodeSkeleton.AttributeNameKey) ~ maxStackGrammar ~ maxLocalGrammar
    val instructionsGrammar = "instructions:" %> new ManyVertical(instructionGrammar).indent()
    val exceptionTableGrammar = "exceptions:" %> produce(Seq.empty[Any])
    val codeAttributeGrammar = header %
      instructionsGrammar.as(CodeInstructionsKey) %
      attributesGrammar.as(CodeAttributesKey) %
      exceptionTableGrammar.as(CodeExceptionTableKey) asNode CodeKey
    grammars.create(CodeGrammar, codeAttributeGrammar)
  }

  override def constantPoolKey: String = "Code"

  override def description: String = "Adds a new bytecode attribute named code. Its main content is a list of instructions."

  override def getParser(unParsed: Node): ClassFileParser.Parser[Node] = ???
}
