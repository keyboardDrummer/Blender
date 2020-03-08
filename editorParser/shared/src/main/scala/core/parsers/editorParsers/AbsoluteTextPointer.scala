package core.parsers.editorParsers

import core.parsers.core.{Metrics, ParseText, TextPointer}
import core.parsers.strings.SubSequence

import scala.annotation.tailrec
import scala.collection.Searching.{Found, InsertionPoint, SearchResult}
import scala.collection.mutable

object AbsoluteTextPointer {
  def getCachingParser[Result](parseText: ParseText, singleResultParser: SingleResultParser[Result]): CachingParser[Result] = {
    val offsetManager = new ArrayOffsetManager(parseText)
    new CachingParser[Result] {

      override def parse(mayStop: StopFunction, metrics: Metrics) = {
        singleResultParser.parse(offsetManager.getOffsetNode(0), mayStop, metrics)
      }

      override def changeRange(from: Int, until: Int, insertionLength: Int): Unit = {
        offsetManager.changeText(from, until, insertionLength)
      }
    }
  }
}

class AbsoluteTextPointer(val manager: ArrayOffsetManager, var offset: Int) extends TextPointer {

  override var cache = new mutable.HashMap[Any, Any]

  override def drop(amount: Int) = manager.getOffsetNode(amount + offset)

  override def toString = offset.toString

  override def charAt(index: Int) = manager.text.charAt(index)

  override def length = manager.text.length

  override def charSequence = new SubSequence(manager.text, offset)

  override def subSequence(from: Int, until: Int) = manager.text.subSequence(from, until)

  override def lineCharacter = manager.text.getPosition(offset)
}

class ArrayOffsetManager(var text: ParseText) {

  val offsets = mutable.ArrayBuffer.empty[AbsoluteTextPointer]
  val offsetCache = mutable.HashMap.empty[Int, TextPointer]
  def getOffsetNode(offset: Int) = {
    offsetCache.getOrElseUpdate(offset, {
      binarySearch(offset) match {
        case Found(index) => offsets(index)
        case InsertionPoint(insertionPoint) =>
          val result = new AbsoluteTextPointer(this, offset)
          offsets.insert(insertionPoint, result)
          result
      }
    })
  }

  @tailrec
  private[this] def binarySearch(offset: Int, from: Int = 0, to: Int = offsets.length): SearchResult = {
    if (to <= from) InsertionPoint(from)
    else {
      val idx = from + (to - from - 1) / 2
      Integer.compare(offset, offsets(idx).offset) match {
        case -1 => binarySearch(offset, from, idx)
        case  1 => binarySearch(offset, idx + 1, to)
        case  _ => Found(idx)
      }
    }
  }

  def changeText(from: Int, until: Int, insertLength: Int): Unit = {
    offsetCache.clear()

    val delta = insertLength - (until - from)
    for(offset <- offsets.sortBy(o => -o.offset)) {
      val absoluteOffset = offset.offset

      val entries = offset.cache.toList
      for(entry <- entries) {
        val entryStart = offset.offset
        val parseResults = entry._2.asInstanceOf[CachingParseResult]
        val entryEnd = Math.max(entryStart + 1, parseResults.latestRemainder.offset)
        val entryIntersectsWithRemoval = from <= entryEnd && entryStart < until
        if (entryIntersectsWithRemoval) {
          offset.cache.remove(entry._1)
        }
      }
      if (absoluteOffset > from) {
        offset.offset += delta
      }
      if (absoluteOffset == from) {
        val newNode = getOffsetNode(offset.offset + delta)
        newNode.cache = offset.cache
        offset.cache = new mutable.HashMap[Any, Any]()
      }
    }
  }

  def clear(): Unit = {
    offsets.clear()
    offsetCache.clear()
  }
}