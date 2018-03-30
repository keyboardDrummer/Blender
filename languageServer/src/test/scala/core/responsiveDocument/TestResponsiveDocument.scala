package core.responsiveDocument

import core.responsiveDocument.ResponsiveDocument._
import org.junit.{Assert, Test}
import org.scalatest.FunSuite

class TestResponsiveDocument extends FunSuite {

  val newLine = System.lineSeparator()

  test("Text")
  {
    val expected: String = "hallo"
    val document: ResponsiveDocument = text(expected)
    assertResult(expected)(document.renderString())
  }

  test("LeftRight")
  {
    val expected = "hallo" + "daar"
    val document = ("hallo": ResponsiveDocument) ~ "daar"
    assertResult(expected)(document.renderString())
  }

  test("TopBottom")
  {
    val expected = "aa" + newLine + "b"
    val document = text("aa") % "b"
    assertResult(expected)(document.renderString())
  }

  test("TopBottomBottomWider")
  {
    val expected = "a" + newLine + "bb"
    val document = text("a") % "bb"
    assertResult(expected)(document.renderString())
  }
}
