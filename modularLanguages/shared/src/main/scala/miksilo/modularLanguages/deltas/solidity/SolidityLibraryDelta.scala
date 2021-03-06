package miksilo.modularLanguages.deltas.solidity

import miksilo.modularLanguages.core.deltas.Delta
import miksilo.modularLanguages.core.deltas.path.PathRoot
import miksilo.languageServer.core.language.{CompilationField, Language}
import miksilo.languageServer.core.smarts.objects.NamedDeclaration
import miksilo.languageServer.core.smarts.types.objects.{Type, TypeFromDeclaration}
import miksilo.modularLanguages.core.SolveConstraintsDelta
import miksilo.modularLanguages.deltas.ConstraintSkeleton
import miksilo.modularLanguages.deltas.bytecode.types.TypeSkeleton
import miksilo.modularLanguages.deltas.classes.constructor.ConstructorDelta
import miksilo.modularLanguages.deltas.javac.types.BooleanTypeDelta

object SolidityLibraryDelta extends Delta {

  val addressDeclaration = new CompilationField[NamedDeclaration]()
  override def inject(language: Language): Unit = {
    SolveConstraintsDelta.constraintCollector.add(language, (compilation, builder) => {
      val rootScope = builder.newScope(debugName = "rootScope")

      val intTypeNode = ElementaryTypeDelta.neww("int")
      val intType = TypeSkeleton.getType(compilation, builder, intTypeNode, rootScope)

      val uint256Node = ElementaryTypeDelta.neww("uint256")
      val uint256 = TypeSkeleton.getType(compilation, builder, uint256Node, rootScope)

      val blockTypeDeclaration = builder.declare("<blockType>", rootScope)
      val blockType = TypeFromDeclaration(blockTypeDeclaration)
      builder.declare("block", rootScope, null, Some(blockType))
      val blockScope = builder.declareScope(blockTypeDeclaration)
      builder.declare("timestamp", blockScope, null, Some(uint256))

      val stringNode = ElementaryTypeDelta.neww("string")
      val bytesNode = ElementaryTypeDelta.neww("bytes")
      val stringToBytesConversionType = SolidityFunctionTypeDelta.createType(compilation, builder, rootScope, Seq(stringNode), Seq(bytesNode))
      builder.declare("bytes", rootScope, _type = Some(stringToBytesConversionType))

      val bytesToStringConversionType = SolidityFunctionTypeDelta.createType(compilation, builder, rootScope, Seq(bytesNode), Seq(stringNode))
      builder.declare("string", rootScope, _type = Some(bytesToStringConversionType))

      val uintToIntConversionType = SolidityFunctionTypeDelta.createType(Seq[Type](uint256), Seq[Type](intType))
      builder.declare("int", rootScope, _type = Some(uintToIntConversionType))

      val addressDeclaration = builder.declare("address", rootScope, null, Some(TypeSkeleton.typeKind))
      this.addressDeclaration(compilation) = addressDeclaration
      val addressScope = builder.declareScope(addressDeclaration)

      val stringDeclaration = builder.resolveToType("string", null, rootScope, TypeSkeleton.typeKind)
      val stringScope = builder.getDeclaredScope(stringDeclaration)
      val stringConstructorType = SolidityFunctionTypeDelta.createType(compilation, builder, rootScope, Seq(uint256Node), Seq.empty)
      builder.declare(ConstructorDelta.constructorName, stringScope, null, Some(stringConstructorType))

      builder.declare("balance", addressScope, null, Some(uint256))
      val transferType = SolidityFunctionTypeDelta.createType(compilation, builder, rootScope, Seq(uint256Node), Seq.empty)
      builder.declare("transfer", addressScope, null, Some(transferType))

      val msgType = builder.declare("<MSGDECLARATION>", rootScope) // TODO get rid of fake declarations
      val msgScope = builder.declareScope(msgType, rootScope, "msgScope")
      val message = builder.declare("msg", rootScope, _type = Some(TypeFromDeclaration(msgType)))

      builder.declare("data", msgScope)
      builder.declare("gas", msgScope)
      builder.declare("sender", msgScope)
      builder.declare("sig", msgScope)
      builder.declare("value", msgScope)

      val assertType = SolidityFunctionTypeDelta.createType(compilation, builder, rootScope, Seq(BooleanTypeDelta.booleanType), Seq.empty)
      builder.declare("assert", rootScope, _type = Some(assertType))

      builder.declare("require", rootScope, _type = Some(assertType))

      val revertType = SolidityFunctionTypeDelta.createType(compilation, builder, rootScope, Seq.empty, Seq.empty)
      builder.declare("revert", rootScope, _type = Some(revertType))

      builder.declare("now", rootScope, null, Some(uint256))
      ConstraintSkeleton.constraints(compilation, builder, compilation.program.asInstanceOf[PathRoot], rootScope)
    })
  }

  override def description = "Adds the solidity standard library"

  override def dependencies = Set.empty
}
