package Yosemite.framework.master

import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import Yosemite.framework.{FlowDescription, JobDescription}
import Yosemite.framework.slave.SlaveInfo
import akka.actor.ActorRef

import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, HashMap, Map}

/**
  * Created by zhanghan on 17/4/6.
  */


private [Yosemite] class JobInfo(var startTime:Long, var id:String,var desc:JobDescription,
                                var starter:SlaveInfo,val submiteTime:Date,val actor:ActorRef) {

  private var _prevState=JobState.WAITING
  def prevState=_prevState

  private var _currentState=JobState.WAITING
  def currentState=_currentState


  def changeState(nextState:JobState.JobState): Unit ={
    _prevState=_currentState
    _currentState=nextState
  }

  var origAlpha=0.0

  var _bytesLeft = new AtomicLong(0L)
  def bytesLeft= _bytesLeft.get()

  var readyTime = -1L
  var endTime = -1L

  var allocationOverTime = new ArrayBuffer[(Long, Double)]()
  allocationOverTime += ((startTime, 0.0))

  private var _retryCount = 0
  def retryCount = _retryCount

  private val numRegisteredFlows = new AtomicInteger(0)
  private val numCompletedFlows = new AtomicInteger(0)

  def numFlowsToRegister = desc.width - numRegisteredFlows.get
  def numFlowsToComplete = desc.width - numCompletedFlows.get

  private val idToFlow = new ConcurrentHashMap[String, FlowInfo]()

  def getFlows() = idToFlow.values.asScala.filter(_.isaLive)

  def getFlowInfos(flowIds: Array[String]): Option[Array[FlowInfo]] = {
    val ret = flowIds.map(idToFlow.get(_))
    ret.foreach(v => {
      if (v == null)
        return None
    })
    Some(ret)
  }

  def getFlowInfo(flowId: String): Option[FlowInfo] = {
    val ret = idToFlow.get(flowId)
    if (ret == null) None else Some(ret)
  }

  def contains(flowId: String) = idToFlow.containsKey(flowId)

  /**
    * Calculate remaining time based on remaining flow size
    */
  def calcRemainingMillis(sBpsFree: Map[String, Double], rBpsFree: Map[String, Double]): Double = {
    val sBytes = new HashMap[String, Double]().withDefaultValue(0.0)
    val rBytes = new HashMap[String, Double]().withDefaultValue(0.0)

    getFlows.foreach { flowInfo =>
      // FIXME: Assuming a single source and destination for each flow
      val src = flowInfo.sourceip
      val dst = flowInfo.dstinationslave.IP

      sBytes(src) = sBytes(src) + flowInfo.bytesLeft
      rBytes(dst) = rBytes(dst) + flowInfo.bytesLeft
    }

    // Scale by available capacities
    for ((src, v) <- sBytes) {
      sBytes(src) = v * 8.0 / sBpsFree(src)
    }
    for ((dst, v) <- rBytes) {
      rBytes(dst) = v * 8.0 / rBpsFree(dst)
    }

    math.max(sBytes.values.max, rBytes.values.max) * 1000
  }




  /**
    * Calculating alpha for the coflow based on remaining flow size
    */
  def calcAlpha(): Double = {
    val sBytes = new HashMap[String, Double]().withDefaultValue(0.0)
    val rBytes = new HashMap[String, Double]().withDefaultValue(0.0)

    getFlows.foreach { flowInfo =>
      // FIXME: Assuming a single source and destination for each flow
      val src = flowInfo.sourceip
      val dst = flowInfo.dstinationslave.IP

      sBytes(src) = sBytes(src) + flowInfo.bytesLeft
      rBytes(dst) = rBytes(dst) + flowInfo.bytesLeft
    }
    math.max(sBytes.values.max, rBytes.values.max)
  }

  def addFlow(flowDesc: FlowDescription) {
    assert(!idToFlow.containsKey(flowDesc.id))
    idToFlow.put(flowDesc.id, new FlowInfo(flowDesc))
    _bytesLeft.getAndAdd(flowDesc.size)
  }




  /**
    * Adds destination for a given piece of data.
    * Assume flowId already exists in idToFlow
    * Returns true if the coflow is ready to go
    */
  def addDestination(flowId: String, destClient: SlaveInfo): Boolean = {
    if (idToFlow.get(flowId).dstinationslave == null) {
      numRegisteredFlows.getAndIncrement
    }
    idToFlow.get(flowId).setDestination(destClient)
    postProcessIfReady
  }




  /**
    * Mark this coflow as RUNNING only after all flows are alive
    * Returns true if the coflow is ready to go
    */
  private def postProcessIfReady(): Boolean = {
    if (numRegisteredFlows.get == desc.width) {
      origAlpha = calcAlpha()
      changeState(JobState.READY)
      readyTime = System.currentTimeMillis
      true
    } else {
      false
    }
  }






  /**
    * Keep track of allocation over time
    */
  def setCurrentAllocation(newTotalBps: Double) = {
    allocationOverTime += ((System.currentTimeMillis, newTotalBps))
  }

  def currentAllocation(): (Long, Double) = allocationOverTime.last




  /**
    * Adds destinations for a multiple pieces of data.
    * Assume flowId already exists in idToFlow
    * Returns true if the coflow is ready to go
    */
  def addDestinations(flowIds: Array[String], destClient: SlaveInfo): Boolean = {
    flowIds.foreach { flowId =>
      if (idToFlow.get(flowId).dstinationslave == null) {
        numRegisteredFlows.getAndIncrement
      }
      idToFlow.get(flowId).setDestination(destClient)
    }
    postProcessIfReady
  }




  /**
    * Updates bytes remaining in the specified flow
    * Returns true if the flow has completed; false otherwise
    */
  def updateFlow(flowId: String, bytesSinceLastUpdate: Long, isCompleted: Boolean): Boolean = {
    val flow = idToFlow.get(flowId)
    flow.descreaseBytes(bytesSinceLastUpdate)
    _bytesLeft.getAndAdd(-bytesSinceLastUpdate)
    if (isCompleted) {
      assert(flow.bytesLeft == 0)
      numCompletedFlows.getAndIncrement
      true
    }
    false
  }


  def removeFlow(flowId: String) {
    // TODO:
  }

  def incrementRetryCount = {
    _retryCount += 1
    _retryCount
  }

  def markFinished(endState: JobState.Value) {
    changeState(endState)
    endTime = System.currentTimeMillis()
  }

  /**
    * Returns an estimation of remaining bytes
    */
  def remainingSizeInBytes(): Double = {
    bytesLeft.toDouble
  }

  def duration: Long = {
    if (endTime != -1) {
      endTime - startTime
    } else {
      System.currentTimeMillis() - startTime
    }
  }

  override def toString: String = "CoflowInfo(" + id + "[" + desc + "], state=" + currentState +
    ", numRegisteredFlows=" + numRegisteredFlows.get + ", numCompletedFlows=" +
    numCompletedFlows.get + ", bytesLeft= " + bytesLeft + ")"
}