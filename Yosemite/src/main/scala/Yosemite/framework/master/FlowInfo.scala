package Yosemite.framework.master

import java.util.concurrent.atomic.AtomicLong

import Yosemite.framework.FlowDescription
import Yosemite.framework.slave.SlaveInfo

/**
  * Created by zhanghan on 17/4/6.
  */
private [Yosemite] class FlowInfo(val flowdesc: FlowDescription) {


  var sourceip = flowdesc.src



  var sourceport = flowdesc.srcPort




  var bytesLeft_  =  new AtomicLong(flowdesc.size)




  def bytesLeft:Long = bytesLeft_.get()




  var dstinationslave:SlaveInfo= null




  def setDestination(dslave:SlaveInfo){
    dstinationslave = dslave
  }



  def isaLive=(dstinationslave != null && bytesLeft  > 0)


  def getFlowSize()=flowdesc.size


  def descreaseBytes(byteToDescrise:Long): Unit ={
    bytesLeft_.getAndAdd(-byteToDescrise)
  }



  override def toString: String = "flow information("+flowdesc.src+" "+flowdesc.srcPort+" "+flowdesc.dst+" "+flowdesc.dstPort+" "+bytesLeft+")"
}
