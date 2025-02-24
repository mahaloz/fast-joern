package io.shiftleft.semanticcpg

import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.Properties
import io.shiftleft.passes.SimpleCpgPass
import io.shiftleft.semanticcpg.language._
import overflowdb.BatchedUpdate

object Overlays {

  def appendOverlayName(cpg: Cpg, overlayName: String): Unit = {
    new SimpleCpgPass(cpg) {
      override def run(diffGraph: BatchedUpdate.DiffGraphBuilder): Unit = {
        cpg.metaData.headOption match {
          case Some(metaData) =>
            val newValue = metaData.overlays :+ overlayName
            diffGraph.setNodeProperty(metaData, Properties.OVERLAYS.name, newValue)
          case None =>
            System.err.println("Missing metaData block")
        }
      }
    }.createAndApply()
  }

  def removeLastOverlayName(cpg: Cpg): Unit = {
    new SimpleCpgPass(cpg) {
      override def run(diffGraph: BatchedUpdate.DiffGraphBuilder): Unit = {
        cpg.metaData.headOption match {
          case Some(metaData) =>
            val newValue = metaData.overlays.dropRight(1)
            diffGraph.setNodeProperty(metaData, Properties.OVERLAYS.name, newValue)
          case None =>
            System.err.println("Missing metaData block")
        }
      }
    }.createAndApply()
  }

  def appliedOverlays(cpg: Cpg): Seq[String] = {
    cpg.metaData.headOption match {
      case Some(metaData) => Option(metaData.overlays).getOrElse(Nil)
      case None =>
        System.err.println("Missing metaData block")
        List()
    }
  }

}
