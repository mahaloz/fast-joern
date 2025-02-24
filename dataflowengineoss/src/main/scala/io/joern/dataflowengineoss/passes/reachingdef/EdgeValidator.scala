package io.joern.dataflowengineoss.passes.reachingdef

import io.joern.dataflowengineoss.language._
import io.joern.dataflowengineoss.queryengine.Engine.isOutputArgOfInternalMethod
import io.joern.dataflowengineoss.semanticsloader.Semantics
import io.shiftleft.codepropertygraph.generated.nodes.{Call, CfgNode, Expression, StoredNode}
import io.shiftleft.semanticcpg.language._
import overflowdb.traversal._

object EdgeValidator {

  /** Determines whether the edge from `parentNode`to `childNode` is valid, according to the given semantics.
    */
  def isValidEdge(childNode: CfgNode, parentNode: CfgNode)(implicit semantics: Semantics): Boolean =
    (childNode, parentNode) match {
      case (childNode: Expression, parentNode)
          if isCallRetval(parentNode) || !isValidEdgeToExpression(parentNode, childNode) =>
        false
      case (_: Expression, _: Expression)                  => true
      case (childNode: Expression, _) if !childNode.isUsed => false
      case (_: Expression, _)                              => true
      case (_, parentNode)                                 => !isCallRetval(parentNode)
    }

  private def isValidEdgeToExpression(parNode: CfgNode, curNode: Expression)(implicit semantics: Semantics): Boolean =
    parNode match {
      case parentNode: Expression =>
        val sameCallSite = parentNode.inCall.l == curNode.start.inCall.l
        !(sameCallSite && isOutputArgOfInternalMethod(parentNode)) &&
        (sameCallSite && parentNode.isUsed && curNode.isDefined || !sameCallSite && curNode.isUsed)
      case _ =>
        curNode.isUsed
    }

  private def isCallRetval(parentNode: StoredNode)(implicit semantics: Semantics): Boolean =
    parentNode match {
      case call: Call =>
        val sem = semantics.forMethod(call.methodFullName)
        sem.isDefined && !sem.get.mappings.map(_._2).contains(-1)
      case _ =>
        false
    }

}
