package io.joern.dataflowengineoss.queryengine

import io.joern.dataflowengineoss.queryengine.Engine.argToOutputParams
import io.shiftleft.codepropertygraph.generated.nodes.{Call, CfgNode, Expression, MethodParameterIn, MethodParameterOut}
import io.shiftleft.semanticcpg.language.NoResolve
import overflowdb.traversal.Traversal
import io.shiftleft.semanticcpg.language._

/** Creation of new tasks from results of completed tasks.
  */
class TaskCreator(sources: Set[CfgNode]) {

  /** For a given list of results and sources, generate new tasks.
    */
  def createFromResults(results: Vector[ReachableByResult]): Vector[ReachableByTask] =
    tasksForParams(results) ++ tasksForUnresolvedOutArgs(results)

  /** Create new tasks from all results that start in a parameter. In essence, we want to traverse to corresponding
    * arguments of call sites, but we need to be careful here not to create unrealizable paths. We achieve this
    * by holding a call stack in results.
    *
    * Case 1: walking backward from the sink, we have only expanded into callers so far, that is, the call stack
    * is empty. In this case, the next tasks need to explore each call site to the method.
    *
    * Case 2: we expanded into a callee that we identified on the way, e.g., a method `y = transform(x)`, and we have
    * reached the parameter of that method (`transform`). Upon doing so, we recorded the call site that we expanded in
    * `result.callSite`. We would now like to continue exploring from the corresponding argument at that call site only.
    */
  private def tasksForParams(results: Vector[ReachableByResult]): Vector[ReachableByTask] = {
    startsAtParameter(results).flatMap { result =>
      val param = result.path.head.node.asInstanceOf[MethodParameterIn]
      if (result.callSiteStack.isEmpty) {
        // Case 1
        paramToArgs(param).map { arg =>
          ReachableByTask(arg, sources, new ResultTable, result.path, result.callDepth + 1)
        }
      } else {
        // Case 2
        val callSite = result.callSiteStack.pop()
        paramToArgs(param).filter(x => x.inCall.exists(c => c == callSite)).map { arg =>
            ReachableByTask(arg, sources, new ResultTable, result.path, result.callDepth + 1, result.callSiteStack.clone())
        }
      }
    }
  }

  /** Returns only those results that start at a parameter node.
    */
  private def startsAtParameter(results: Vector[ReachableByResult]) = {
    results.collect { case r: ReachableByResult if r.path.head.node.isInstanceOf[MethodParameterIn] => r }
  }


  /** For a given parameter of a method, determine all corresponding arguments at all call sites to the method.
    */
  private def paramToArgs(param: MethodParameterIn): List[Expression] =
    NoResolve
      .getMethodCallsites(param.method)
      .to(Traversal)
      .collectAll[Call]
      .argument(param.index)
      .l

  /**
    * Create new tasks from all results that end in an output argument, including return arguments.
    * In this case, we want to traverse to corresponding method output parameters and method return
    * nodes respectively.
    * */
  private def tasksForUnresolvedOutArgs(results: Vector[ReachableByResult]): Vector[ReachableByTask] = {

    val outArgsAndCalls = results
      .map(x => (x, x.outputArgument, x.path, x.callDepth))
      .distinct

    val forCalls = outArgsAndCalls.flatMap { case (result, outArg, path, callDepth) =>
      val outCall = outArg.collect { case n: Call => n }
      val methodReturns = outCall.toList
        .flatMap(x => NoResolve.getCalledMethods(x).methodReturn.map(y => (x, y)))
        .to(Traversal)

      methodReturns.map { case (call, ret) =>
        val newPath = Vector(path.head.copy(isOutputArg = true)) ++ path.tail
        val callSiteStack = result.callSiteStack.clone()
        callSiteStack.push(call)
        ReachableByTask(ret, sources, new ResultTable, newPath, callDepth + 1, callSiteStack)
      }
    }

    val forArgs = outArgsAndCalls.flatMap { case (result, args, path, callDepth) =>
      args.toList.flatMap { arg =>
        val outParams = if (result.callSiteStack.nonEmpty) {
          List[MethodParameterOut]()
        } else {
          argToOutputParams(arg.asInstanceOf[Expression]).l
        }
        val newPath = Vector(path.head.copy(isOutputArg = true)) ++ path.tail
        outParams
          .map { p =>
            val callSiteStack = result.callSiteStack.clone()
            arg.asInstanceOf[Expression].inCall.headOption.foreach{x => callSiteStack.push(x)}
            ReachableByTask(p, sources, new ResultTable, newPath, callDepth + 1, callSiteStack)
          }
      }
    }

    forCalls ++ forArgs
  }

}
