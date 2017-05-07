package Yosemite.framework.master.scheduler
import Yosemite.Logging
import Yosemite.framework.master.{CoflowInfo, FlowInfo}

import scala.collection.mutable.ArrayBuffer
/**
  * Implementation of the Shortest-Effective-Bottleneck-First coflow scheduler.
  * It sorts coflows by expected CCT based on current bottlebneck duration.
  * All coflows are accepted; hence, there is no admission control.
  */
class SEBFScheduler extends OrderingBasedScheduler with Logging {

  override def getOrderedCoflows(
                                  activeCoflows: ArrayBuffer[CoflowInfo]): ArrayBuffer[CoflowInfo] = {

      //println(activeCoflows.size)

      logInfo("scheduling active coflows"+activeCoflows.size)
    var t=activeCoflows.iterator
    while(t.hasNext){
      var k = t.next()
      logInfo("show detail of active coflows"+k.calcAlpha())
    }

      activeCoflows.sortWith(_.calcAlpha < _.calcAlpha)
  }

  override def calcFlowRate(
                             flowInfo: FlowInfo,
                             cf: CoflowInfo,
                             minFree: Double): Double = {

    minFree * (flowInfo.getFlowSize() / cf.origAlpha)
  }
}
