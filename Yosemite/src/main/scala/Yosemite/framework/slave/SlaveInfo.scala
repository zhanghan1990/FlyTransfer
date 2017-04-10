package Yosemite.framework.slave

import Yosemite.framework.master.JobInfo
import akka.actor.ActorRef

import scala.collection.immutable.HashSet


/**
  * Created by zhanghan on 17/4/6.
  * Slave description: to describe the slave information
  */
private [Yosemite] class SlaveInfo(val startTime:Long,val id:String,val IP: String,
                                  val Port:Int, val actor: ActorRef) {

  var Jobs= new HashSet[JobInfo]
  var endTime = -1L

  def markEndTime(){
    endTime= System.currentTimeMillis()
  }

  def getDuration:Long={
    if(endTime != -1){
      endTime-startTime
    }
    else{
      System.currentTimeMillis()-startTime
    }
  }

  def addJob(jobinfo: JobInfo)={
    Jobs+=jobinfo
  }

  override def toString: String = "startTime:"+startTime+" id"+id+" IP"+IP+" Port"+Port
}
