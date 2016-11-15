package util

import java.lang.reflect.Method

import scala.collection.mutable

class ExtendedType[T](_type: Class[T])
{
  def properties: Seq[Property[T, AnyRef]] = {
    val getters = _type.getMethods.filter(method => method.getParameterCount == 0 && method.getReturnType != null)
    val methodsByName = _type.getMethods.map(method => (method.getName,method)).toMap
    val properties: mutable.ArraySeq[Property[T, AnyRef]] = getters.flatMap(getter => getterToProperty(methodsByName, getter))
    properties
  }

  def getterToProperty(methodsByName: Map[String, Method], getter: Method): Option[Property[T, AnyRef]] = {

    val name = getter.getName
    val propertyType = getter.getReturnType
    val setterOption = methodsByName.get(name + "_$eq")
    if (setterOption.isEmpty)
      return None

    val setter = setterOption.get
    if (!setter.getReturnType.equals(Void.TYPE) || setter.getParameterCount != 1 || setter.getParameterTypes()(0) != propertyType)
      return None

    Some(new Property[T, AnyRef] {
      override def get(obj: T): AnyRef = getter.invoke(obj)

      override def set(obj: T, value: AnyRef): Unit = setter.invoke(obj, value)

      override def _type: Class[AnyRef] = propertyType.asInstanceOf[Class[AnyRef]]


    })
  }
}
