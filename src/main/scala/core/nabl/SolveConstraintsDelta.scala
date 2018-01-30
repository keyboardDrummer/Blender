package core.nabl

import core.deltas.Delta
import core.deltas.exceptions.BadInputException
import core.language.{Language, Phase}

import scala.util.{Failure, Success}

object SolveConstraintsDelta extends Delta {

  override def inject(language: Language): Unit = {
    super.inject(language)
    language.compilerPhases ::= Phase(this, compilation => {
      val factory = new Factory()
      val builder = new ConstraintBuilder(factory)
      language.collectConstraints(compilation, builder)

      val solver = new ConstraintSolver(builder, builder.getConstraints,
        allowDuplicateDeclaration = true //TODO figure out what to do with this.
      )
      solver.run() match {
        case Success(_) => compilation.proofs = solver
        case Failure(e:SolveException) => throw ConstraintException(e)
        case Failure(e) => throw e
      }

      compilation.proofs = solver
    })
  }

  case class ConstraintException(solveException: SolveException) extends BadInputException {
    override def toString: String = solveException.toString
  }

  override def description: String = "Adds the go to definition capability"
}