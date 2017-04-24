package Yosemite.framework.scheduler

import Yosemite.Logging
import Yosemite.framework.master.{FlowInfo, JobInfo}

import scala.collection.mutable.ArrayBuffer

/**
 * Implementation of the Shortest-Effective-Bottleneck-First coflow scheduler.
 * It sorts coflows by expected CCT based on current bottlebneck duration.
 * All coflows are accepted; hence, there is no admission control.
 */
class SEBFScheduler extends OrderingBasedScheduler with Logging {

  override def getOrderedJobs(
      activeCoflows: ArrayBuffer[JobInfo]): ArrayBuffer[JobInfo] = {
    activeCoflows.sortWith(_.calcAlpha < _.calcAlpha)
  }

  override def calcFlowRate(
      flowInfo: FlowInfo,
      cf: JobInfo,
      minFree: Double): Double = {

    minFree * (flowInfo.getFlowSize() / cf.origAlpha)
  }
}
