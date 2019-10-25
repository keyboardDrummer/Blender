package core.parsers.core

object StronglyConnectedComponents {
  def computeComponents[T](allVertices: Set[T], getChildren: T => Set[T]) : List[Set[T]] = {
    // The first part is a shameless adaptation from Wikipedia

    var index = 0
    var indices  : Map[T,Int] = Map.empty
    var lowLinks : Map[T,Int] = Map.empty
    var components : List[Set[T]] = Nil
    var s : List[T] = Nil

    def strongConnect(v: T) {
      indices  = indices.updated(v, index)
      lowLinks = lowLinks.updated(v, index)
      index += 1
      s = v :: s

      for (w <- getChildren(v)) {
        if (!indices.isDefinedAt(w)) {
          strongConnect(w)
          lowLinks = lowLinks.updated(v, lowLinks(v) min lowLinks(w))
        } else if (s.contains(w)) {
          lowLinks = lowLinks.updated(v, lowLinks(v) min indices(w))
        }
      }

      if (lowLinks(v) == indices(v)) {
        var c : Set[T] = Set.empty
        var stop = false
        do {
          val x :: xs = s
          c = c + x
          s = xs
          stop = x == v
        } while (!stop)

        components = c :: components
      }
    }

    for (v <- allVertices) {
      if (!indices.isDefinedAt(v)) {
        strongConnect(v)
      }
    }

    components.reverse
  }
}
