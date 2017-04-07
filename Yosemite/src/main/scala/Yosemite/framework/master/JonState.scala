package Yosemite.framework.master

import java.util

/**
  * Created by zhanghan on 17/4/6.
  */
private[Yosemite] object JobState  extends util.Enumeration{
  type JobState=Value
  val WAITING, READY, RUNNING, FINISHED, FAILED, REJECTED = Value

}
