package transformations.javac

import core.bigrammar._
import core.grammar.RegexG
import core.particles.DeltaWithGrammar
import core.particles.grammars.{GrammarCatalogue, ProgramGrammar}
import core.particles.node.Key
import scala.util.matching.Regex

object JavaStyleCommentsC extends DeltaWithGrammar {

  case class CommentCollection(comments: Seq[String])

  object CommentKey extends Key
  object CommentGrammar
  override def transformGrammars(grammars: GrammarCatalogue): Unit = {
    val commentsGrammar = grammars.create(CommentGrammar, getCommentsGrammar.as(CommentKey))

    for(path <- new RootGrammar(grammars.find(ProgramGrammar)).selfAndDescendants.
      filter(path => path.get.isInstanceOf[NodeGrammar]))
    {
      addCommentPrefixToGrammar(commentsGrammar, path.children.head)
    }
  }

  def addCommentPrefixToGrammar(commentsGrammar: BiGrammar, grammarReference: GrammarReference): Unit = {
    val verticalNotHorizontal: Boolean = getCommentVerticalOrHorizontal(grammarReference)
    val newGrammar = if (verticalNotHorizontal) commentsGrammar %> grammarReference.get
                           else commentsGrammar ~> grammarReference.get
    grammarReference.set(newGrammar)
  }

  def getCommentVerticalOrHorizontal(nodeMapPath: GrammarReference): Boolean = {
    val growers = nodeMapPath.ancestors.map(path => path.get).
      filter(grammar => grammar.isInstanceOf[TopBottom] || grammar.isInstanceOf[Sequence] || grammar.isInstanceOf[ManyVertical] || grammar.isInstanceOf[ManyHorizontal])

    val verticalNotHorizontal = growers.nonEmpty && {
      val firstGrower = growers.head
      firstGrower.isInstanceOf[TopBottom] || firstGrower.isInstanceOf[ManyVertical]
    }
    verticalNotHorizontal
  }

  def getCommentsGrammar: BiGrammar = {
    val commentGrammar = getCommentGrammar
    commentGrammar.manyVertical ^^
      (comments => CommentCollection(comments.asInstanceOf[Seq[String]]),
        commentCollection => Some(commentCollection.asInstanceOf[CommentCollection].comments))
  }

  def getCommentGrammar: BiGrammar = {
    val regex: Regex = new Regex( """/\*.*\*/""")
    RegexG(regex) ~ space
  }

  override def description: String = "Adds Java-style comments to the language"
}
