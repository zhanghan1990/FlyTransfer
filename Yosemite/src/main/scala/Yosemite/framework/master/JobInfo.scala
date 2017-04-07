package Yosemite.framework.master

import java.util.Date
import java.util.concurrent.ConcurrentHashMap

import Yosemite.framework.JobDescription

import akka.actor.ActorRef

/**
  * Created by zhanghan on 17/4/6.
  */
private [Yosemite] class JobInfo(var startTime:Long, var id:String,var desc:JobDescription,
                                var starter:SlaveInfo,val submiteTime:Date,val actor:ActorRef) {

  private var prevState=JobState.WAITING
  private val currentState=JobState.WAITING

  private val idToFlow = new ConcurrentHashMap[String, FlowInfo]()





}
