package Yosemite.framework.master.scheduler

import Yosemite.Logging
import Yosemite.framework.master.{CoflowInfo, FlowInfo}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by zhanghan on 17/5/7.
  */
class YosemiteScheduler  extends OrderingBasedScheduler with Logging {
  /**
    * Returns an ordered list of coflows based on the scheduling policy
    */
  override def getOrderedCoflows(activeCoflows: ArrayBuffer[CoflowInfo]): ArrayBuffer[CoflowInfo] ={
    logInfo("beginning to sort at getOrderdCoflows")
    activeCoflows.sortWith(_.calcAlpha >_.calcAlpha)
  }

  /**
    * Calculate rate of an individual flow based on the scheduling policy
    */
  override def calcFlowRate(
                             flowInfo: FlowInfo,
                             cf: CoflowInfo,
                             minFree: Double): Double = {

    minFree * (flowInfo.getFlowSize() / cf.origAlpha)
  }
}
