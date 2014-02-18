package languages.java.base

import transformation.MetaObject
import languages.bytecode.ByteCode

object JavaClassModel {

  def clazz(name: String, methods: Seq[MetaObject]) = new MetaObject(ByteCode.ClassFileKey) {
    data.put(ByteCode.ClassMethodsKey, methods)
    data.put(ByteCode.ClassNameKey, name)
  }
  def getClassName(clazz: MetaObject) = clazz(ByteCode.ClassNameKey).asInstanceOf[String]
  def getMethods(clazz: MetaObject) = clazz(ByteCode.ClassMethodsKey).asInstanceOf[Seq[MetaObject]]
}

object JavaMethodModel {

  object Return
  object ReturnValue
  def _return(value: MetaObject): MetaObject = new MetaObject(Return) {
    data.put(ReturnValue, value)
  }
  def getReturnValue(_return: MetaObject) = _return(ReturnValue).asInstanceOf[MetaObject]

  def getMethodBody(metaObject: MetaObject) = metaObject(MethodBodyKey).asInstanceOf[Seq[MetaObject]]

  object MethodBodyKey
  def method(name: String, _returnType: Any, _parameters: Seq[MetaObject], _body: Seq[MetaObject],
             static: Boolean = false, visibility: String = privateVisibility) = {
    new MetaObject(ByteCode.MethodInfoKey) {
      data.put(MethodNameKey, name)
      data.put(ReturnTypeKey, _returnType)
      data.put(MethodParametersKey, _parameters)
      data.put(MethodBodyKey,_body)
      data.put(StaticKey, static)
      data.put(VisibilityKey, visibility)
    }
  }

  object ParameterNameKey

  object StaticKey
  object VisibilityKey
  val publicVisibility = "public"
  val privateVisibility = "private"
  def parameter(name: String, _type: Any) = {
    new MetaObject("JavaParameter") {
      data.put(ParameterNameKey, name)
      data.put(ParameterTypeKey, _type)
    }
  }

  object ReturnTypeKey
  object MethodNameKey

  def getMethodName(method: MetaObject) = {
    method(MethodNameKey).asInstanceOf[String]
  }

  object MethodParametersKey
  def getMethodParameters(metaObject: MetaObject) = {
    metaObject(MethodParametersKey).asInstanceOf[Seq[MetaObject]]
  }

  def getMethodReturnType(metaObject: MetaObject) = {
    metaObject(ReturnTypeKey)
  }


  def getParameterType(metaObject: MetaObject) : Any = metaObject(ParameterTypeKey)
  def getParameterName(metaObject: MetaObject) = metaObject(ParameterNameKey).asInstanceOf[String]
  object ParameterTypeKey

}

object JavaBaseModel {

  object CallKey
  object CallCallee
  object CallArguments
  def call(callee: MetaObject, arguments: Seq[MetaObject] = Seq()) = {
    new MetaObject(CallKey) {
      data.put(CallCallee, callee)
      data.put(CallArguments, arguments)
    }
  }

  def getCallCallee(call: MetaObject) = call(CallCallee).asInstanceOf[MetaObject]
  def getCallArguments(call: MetaObject) = call(CallArguments).asInstanceOf[Seq[MetaObject]]

  object VariableKey
  val variableNameKey = "name"
  def variable(name: String) = {
    new MetaObject(VariableKey) {
      data.put(variableNameKey, name)
    }
  }

  def getVariableName(variable: MetaObject) = variable(variableNameKey).asInstanceOf[String]


  object SelectorKey
  object SelectorObject
  object SelectorMember
  def selector(selectee: MetaObject, member: String) {
    new MetaObject(SelectorKey) {
      data.put(SelectorObject, selectee)
      data.put(SelectorMember, member)
    }
  }
}

object JavaTypes {

  object StringType
  object VoidType
  object IntegerType
  object DoubleType
  def arrayType(elementType: Any) = {
    new MetaObject("arrayType") { data.put("elementType", elementType) }
  }

}
