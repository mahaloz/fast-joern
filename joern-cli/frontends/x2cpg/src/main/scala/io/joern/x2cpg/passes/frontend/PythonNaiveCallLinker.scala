package io.joern.x2cpg.passes.frontend

import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, PropertyNames}
import io.shiftleft.passes.SimpleCpgPass
import io.shiftleft.semanticcpg.language._
import overflowdb.traversal.jIteratortoTraversal

/** Link remaining unlinked Python calls to methods only by their name (not full name)
  * @param cpg
  *   the target code property graph.
  */
class PythonNaiveCallLinker(cpg: Cpg) extends SimpleCpgPass(cpg) {

  override def run(dstGraph: DiffGraphBuilder): Unit = {
    val methodNameToNode = cpg.method.toList.groupBy(_.name)
    def calls            = cpg.call.filter(_.outE(EdgeTypes.CALL).isEmpty)
    for {
      call    <- calls
      methods <- methodNameToNode.get(call.name)
      method  <- methods
    } {
      dstGraph.addEdge(call, method, EdgeTypes.CALL)
      // If we can only find one name with the exact match then we can semi-confidently set it as the full name
      if (methods.size == 1)
        dstGraph.setNodeProperty(call, PropertyNames.METHOD_FULL_NAME, method.fullName)
    }
  }

}
