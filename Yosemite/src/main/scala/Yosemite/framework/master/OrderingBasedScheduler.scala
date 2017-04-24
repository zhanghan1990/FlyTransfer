package Yosemite.framework.scheduler

import Yosemite.Logging
import Yosemite.framework.master.{FlowInfo, JobInfo, JobState}

import scala.collection.mutable.{ArrayBuffer, HashMap, Map}

/**
 * Implementation of a generalized coflow scheduler that works using the 
 * following steps:
 *  1. Order coflows by some criteria.
 *  2. Allocate rates to individual flows of each admitted coflow in that order.
 */
abstract class OrderingBasedScheduler extends JobScheduler with Logging {

  val NIC_BitPS = System.getProperty("Yosemite.network.nicMbps", "1024").toDouble * 1048576.0

  override def schedule(schedulerInput: SchedulerInput): SchedulerOutput = {
    val markedForRejection = new ArrayBuffer[JobInfo]()

    // STEP 1: Sort READY or RUNNING coflows by arrival time
    var sortedJobs = getOrderedJobs(schedulerInput.activeJobs)

    // STEP 2: Perform WSS + Backfilling
    val sBpsFree = new HashMap[String, Double]().withDefaultValue(NIC_BitPS)
    val rBpsFree = new HashMap[String, Double]().withDefaultValue(NIC_BitPS)

    for (cf <- sortedJobs) {
      logInfo("Scheduling " + cf)

      if (markForRejection(cf, sBpsFree, rBpsFree)) {
        markedForRejection += cf
      } else {
        val sUsed = new HashMap[String, Double]().withDefaultValue(0.0)
        val rUsed = new HashMap[String, Double]().withDefaultValue(0.0)

        for (flowInfo <- cf.getFlows) {
          val src = flowInfo.sourceip
          val dst = flowInfo.dstinationslave.IP

          val minFree = math.min(sBpsFree(src), rBpsFree(dst))
          if (minFree > 0.0) {
            flowInfo.currentBps = calcFlowRate(flowInfo, cf, minFree)
            if (math.abs(flowInfo.currentBps) < 1e-6) {
              flowInfo.currentBps = 0.0
            }
            flowInfo.lastScheduled = System.currentTimeMillis

            // Remember how much capacity was allocated
            sUsed(src) = sUsed(src) + flowInfo.currentBps
            rUsed(dst) = rUsed(dst) + flowInfo.currentBps

            // Set the coflow as running
            cf.changeState(JobState.RUNNING)
          } else {
            flowInfo.currentBps = 0.0
          }
        }

        // Remove capacity from ALL sources and destination for this coflow
        for (sl <- schedulerInput.activeSlaves) {
          val host = sl.IP
          sBpsFree(host) = sBpsFree(host) - sUsed(host)
          rBpsFree(host) = rBpsFree(host) - rUsed(host)
        }
      }
    }

    // STEP2A: Work conservation
    sortedJobs = sortedJobs.filter(_.currentState == JobState.RUNNING)
    for (cf <- sortedJobs) {
      var totalBps = 0.0
      for (flowInfo <- cf.getFlows) {
        val src = flowInfo.sourceip
        val dst = flowInfo.dstinationslave.IP

        val minFree = math.min(sBpsFree(src), rBpsFree(dst))
        if (minFree > 0.0) {
          flowInfo.currentBps += minFree
          sBpsFree(src) = sBpsFree(src) - minFree
          rBpsFree(dst) = rBpsFree(dst) - minFree
        }
        
        totalBps += flowInfo.currentBps
      }
      // Update current allocation of the coflow
      cf.setCurrentAllocation(totalBps)
    }

    SchedulerOutput(sortedJobs, markedForRejection)
  }

  /**
   *  Returns an ordered list of coflows based on the scheduling policy
   */
  def getOrderedJobs(
      activeJobs: ArrayBuffer[JobInfo]): ArrayBuffer[JobInfo]

  /**
   * Mark a coflow as non-admissible based on some criteria.
   * Overriden for schedulers with admission control (e.g., DeadlineScheduler)
   */
  def markForRejection(
      cf: JobInfo,
      sBpsFree: Map[String, Double], 
      rBpsFree: Map[String, Double]): Boolean =  false

  /**
   * Calculate rate of an individual flow based on the scheduling policy
   */
  def calcFlowRate(
      flowInfo: FlowInfo,
      cf: JobInfo,
      minFree: Double): Double

  /** Retuns current time */
  def now() = System.currentTimeMillis
}
