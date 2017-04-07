package Yosemite.framework.master

/**
  * Created by zhanghan on 17/4/6.
  */
private[Yosemite] object JobState  extends Enumeration{
  type JobState=Value
  val WAITING, READY, RUNNING, FINISHED, FAILED, REJECTED,TEST = Value

}
