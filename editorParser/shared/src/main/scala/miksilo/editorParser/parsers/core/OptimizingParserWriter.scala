package miksilo.editorParser.parsers.core

import miksilo.editorParser.parsers.editorParsers.{DelayedParseResult, ParseResults, SingleResultParser, SpotlessHistory}

import scala.collection.mutable

trait OptimizingParserWriter extends ParserWriter {

  type State
  def startState: State

  type ParseResult[+Result] = ParseResults[State, Result]
  type Parser[+Result] = ParserBuilder[Result]

  def newParseState(input: TextPointer): FixPointState

  class LoopBreaker[Result](_original: => BuiltParser[Result], maxStackDepth: Int)
    extends BuiltParser[Result] {

    override def apply(input: TextPointer, state: State, fixPointState: FixPointState): ParseResults[State, Result] = {
      if (fixPointState.stackDepth > maxStackDepth) {
        ParseResults.singleResult[State, Result](new DelayedParseResult(input, SpotlessHistory(100000000),
          () => _original.apply(input, state, FixPointState(fixPointState.offset, 0, fixPointState.callStack))))
      } else {
        _original.apply(input, state, FixPointState(fixPointState.offset, fixPointState.stackDepth + 1, fixPointState.callStack))
      }
    }

    override def origin: Option[ParserBuilder[Result]] = None
  }

  case class FixPointState(offset: Int, // TODO try to remove this offset, since we can also clear the callStack whenever we move forward.
                           stackDepth: Int,
                           callStack: Set[BuiltParser[Any]])

  def wrapParser[Result](parser: BuiltParser[Result],
                         shouldCache: Boolean,
                         shouldDetectLeftRecursion: Boolean): BuiltParser[Result]

  trait BuiltParser[+Result] {
    def apply(position: TextPointer, state: State, fixPointState: FixPointState): ParseResult[Result]
    def origin: Option[ParserBuilder[Result]]

    override def toString: String = origin.fold("generated")(o => o.toString)
  }

  trait GetParser {
    def apply[Result](parser: Parser[Result]): BuiltParser[Result]
  }

  trait ParserBuilder[+Result] {
    def getParser(recursive: GetParser): BuiltParser[Result]
    def mustConsumeInput: Boolean
    def getMustConsume(cache: ConsumeCache): Boolean
    def leftChildren: List[ParserBuilder[_]]
    def children: List[ParserBuilder[_]]

    def print(visited: Set[ParserBuilder[Any]], names: mutable.Map[ParserBuilder[Any], Int]): String
  }

  trait ParserBuilderBase[Result] extends ParserBuilder[Result] {
    var mustConsumeInput: Boolean = false

    override def toString: String = print(Set.empty, mutable.HashMap.empty)

    def print(visited: Set[ParserBuilder[Any]], names: mutable.Map[ParserBuilder[Any], Int]): String = {
      if (visited.contains(this)) {
        names.addOne(this -> names.size)
        "ref" + names(this)
      } else {
        val result = printInner(visited + this, names)
        if (names.contains(this)) {
          s"def${names(this)}($result)"
        } else {
          result
        }
      }
    }
    def printInner(visited: Set[ParserBuilder[Any]], names: mutable.Map[ParserBuilder[Any], Int]): String = super.toString
  }

  trait SequenceLike[+Result] extends ParserBuilder[Result] {
    def left: Parser[Any]
    def right: Parser[Any]

    override def children = List(left, right)

    override def leftChildren: List[ParserBuilder[Any]] = if (left.mustConsumeInput) List(left) else List(left, right)

    override def getMustConsume(cache: ConsumeCache) = cache(left) || cache(right)
  }

  trait ChoiceLike[+Result] extends ParserBuilder[Result] {
    def first: Parser[Result]
    def second: Parser[Result]

    override def children = List(first, second)

    override def leftChildren = List(first, second)

    override def getMustConsume(cache: ConsumeCache) = cache(first) && cache(second)
  }

  trait ParserWrapper[+Result] extends ParserBuilder[Result] {
    def original: ParserBuilder[Any]

    override def getMustConsume(cache: ConsumeCache) = cache(original)

    override def leftChildren = List(original)

    override def children = List(original)
  }

  class Lazy[Result](_original: => Parser[Result], val debugName: Any = null)
    extends ParserBuilderBase[Result] with ParserWrapper[Result] {
    lazy val original: Parser[Result] = _original
    def getOriginal = original

    override def getParser(recursive: GetParser): BuiltParser[Result] = {
      lazy val parseOriginal = recursive(original)
      new BuiltParser[Result] {
        override def apply(input: TextPointer, state: State, fixPointState: FixPointState) = {
          parseOriginal(input, state, fixPointState)
        }

        override def origin = Some(Lazy.this)
      }
    }

    override def print(visited: Set[ParserBuilder[Any]], names: mutable.Map[ParserBuilder[Any], Int]): String = printInner(visited, names)
    override def printInner(visited: Set[ParserBuilder[Any]], names: mutable.Map[ParserBuilder[Any], Int]): String =
      if (debugName != null) debugName.toString else original.print(visited, names)

    override def getMustConsume(cache: ConsumeCache) = cache(original)
  }

  trait LeafParser[+Result] extends ParserBuilder[Result] {
    override def leftChildren = List.empty

    override def children = List.empty
  }

  def compile[Result](root: Parser[Result]): ParserAnalysis = {
    var nodesThatShouldDetectLeftRecursion: Set[ParserBuilder[_]] = Set.empty
    val mustConsumeCache = new ConsumeCache

    val reverseGraph = mutable.HashMap.empty[ParserBuilder[_], mutable.Set[ParserBuilder[_]]]
    GraphAlgorithms.depthFirst[ParserBuilder[_]](root,
      node => {
        node.asInstanceOf[ParserBuilderBase[Any]].mustConsumeInput = mustConsumeCache(node)
        node.children
      },
      (_, path: List[ParserBuilder[_]]) => path match {
        case child :: parent :: _ =>
          val incoming = reverseGraph.getOrElseUpdate(child, mutable.HashSet.empty)
          incoming.add(parent)
        case _ =>
      },
      cycle => {
          nodesThatShouldDetectLeftRecursion += cycle.head
      })

    val leftComponents = StronglyConnectedComponents.computeComponents[ParserBuilder[_]](reverseGraph.keys.toSet, node => node.leftChildren.toSet)
    val components = StronglyConnectedComponents.computeComponents[ParserBuilder[_]](reverseGraph.keys.toSet, node => node.children.toSet)
    val recursiveComponents: Seq[Set[ParserBuilder[_]]] = components.filter(c => c.size > 1)
    val nodesInCycle: Set[ParserBuilder[_]] = leftComponents.filter(c => c.size > 1).flatten.toSet

    val nodesWithMultipleIncomingEdges: Set[ParserBuilder[_]] = reverseGraph.filter(e => e._2.size > 1).keys.toSet
    val nodesWithIncomingCycleEdge: Set[ParserBuilder[_]] = reverseGraph.filter(e => e._2.exists(parent => nodesInCycle.contains(parent))).keys.toSet
    val nodesThatShouldCache: Set[ParserBuilder[_]] = nodesWithIncomingCycleEdge ++ nodesWithMultipleIncomingEdges

    ParserAnalysis(nodesThatShouldCache, nodesThatShouldDetectLeftRecursion, recursiveComponents)
  }

  case class ParserAndCaches[Result](parser: BuiltParser[Result])

  case class ParserAnalysis(nodesThatShouldCache: Set[ParserBuilder[_]],
                            nodesThatShouldDetectLeftRecursion: Set[ParserBuilder[_]],
                            recursiveComponents: Seq[Set[ParserBuilder[_]]]) {

    val loopBreakers = recursiveComponents.map(c => (c.head, 300)).toMap

    def buildParser[Result](root: Parser[Result]): ParserAndCaches[Result] = {
      val cacheOfParses = new mutable.HashMap[Parser[Any], BuiltParser[Any]]
      def recursive: GetParser = new GetParser {
        override def apply[SomeResult](_parser: Parser[SomeResult]): BuiltParser[SomeResult] = {
          cacheOfParses.getOrElseUpdate(_parser, {
            val parser = _parser.asInstanceOf[ParserBuilder[SomeResult]]
            val result = parser.getParser(recursive)
            val loopBreaker = loopBreakers.get(parser)
            val wrappedParser = wrapParser(result, nodesThatShouldCache(parser), nodesThatShouldDetectLeftRecursion(parser))

            wrappedParser // if (loopBreaker.nonEmpty) new LoopBreaker(wrappedParser, loopBreaker.get) else wrappedParser
          }).asInstanceOf[BuiltParser[SomeResult]]
        }
      }

      val wrappedRoot = recursive(root)
      ParserAndCaches(wrappedRoot)
    }
  }

  class ConsumeCache {
    var values = new mutable.HashMap[ParserBuilder[Any], Boolean]

    def apply[Result](parser: ParserBuilder[Any]): Boolean = {
      values.get(parser) match {
        case Some(v) => v
        case None =>
          values.put(parser, false)
          val value = parser.getMustConsume(this)
          values.put(parser, value)
          value
      }
    }
  }

  implicit class ParserExtensions[+Result](parser: Parser[Result]) extends super.ParserExtensions(parser) {

    def addAlternative[Other >: Result](getAlternative: (Parser[Other], Parser[Other]) => Parser[Other], debugName: Any = null): Parser[Other] = {
      lazy val result: Parser[Other] = new Lazy(parser | getAlternative(parser, result), debugName)
      result
    }
  }

  def getSingleResultParser[Result](parser: ParserBuilder[Result]): SingleResultParser[Result]
}
